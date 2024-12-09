#include "runtime.h"

enum Rec_kind { Rec_tag };
struct Rec {
  int rc;
  enum Color color;
  int addedPCR;
  enum Rec_kind kind;
  void (*print)();
  struct Rec* rec;
  union {
    struct {  };
  };
};
void $free_Rec(struct Rec* this);
void $decr_Rec(struct Rec* this);
void $markGray_Rec(struct Rec* this);
void $scan_Rec(struct Rec* this);
void $scanBlack_Rec(struct Rec* this);
void $collectWhite_Rec(struct Rec* this);
void $print_Rec(struct Rec* this);
struct Rec* new$Rec(struct Rec* rec);
void $free_Rec(struct Rec* this) {
  fprintf(stderr, "Freeing Rec\n");
  switch (this->kind) {
  case Rec_tag:
    break;
  }
  free(this);
}
void $decr_Rec(struct Rec* this) {
  fprintf(stderr, "Decrementing Rec (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Rec_tag:
      $decr_Rec(this->rec);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_Rec(struct Rec* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Rec_tag:
    this->rec->rc --;
    $markGray_Rec(this->rec);
    break;
  }
}
void $scan_Rec(struct Rec* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Rec(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Rec_tag:
    $scan_Rec(this->rec);
    break;
  }
}
void $scanBlack_Rec(struct Rec* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Rec_tag:
      this->rec->rc ++;
      $scanBlack_Rec(this->rec);
      break;
    }
  }
}
void $collectWhite_Rec(struct Rec* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Rec_tag:
      $collectWhite_Rec(this->rec);
      break;
    }
    fprintf(stderr, "Removing Rec\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Rec;
  }
}
void $print_Rec(struct Rec* this) {
  switch (this->kind) {
  case Rec_tag:
    printf("Rec {");
    printf("rec=");
    $print_Rec(this->rec);
    printf(", ");
    printf("}");
    break;
  }
}
struct Rec* new$Rec(struct Rec* rec) {
  struct Rec* $res = malloc(sizeof (struct Rec));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Rec;
  $res->kind = Rec_tag;
  $res->rec = rec;
  $res->rec->rc ++;
  return $res;
}

#include "runtime.h"

enum List_kind { Cons_tag, Nil_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  union {
    struct { struct List* next_Cons; };
    struct {  };
  };
};
void $free_List(struct List* this);
void $decr_List(struct List* this);
void $markGray_List(struct List* this);
void $scan_List(struct List* this);
void $scanBlack_List(struct List* this);
void $collectWhite_List(struct List* this);
void $print_List(struct List* this);
struct List* new$Cons(struct List* next);
struct List* new$Nil();
int fn$f(struct List* list);
int main();
void $free_List(struct List* this) {
  switch (this->kind) {
  case Cons_tag:
    break;
  case Nil_tag:
    break;
  }
  free(this);
}
void $decr_List(struct List* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Cons_tag:
      $decr_List(this->next_Cons);
      break;
    case Nil_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Cons_tag:
    this->next_Cons->rc --;
    $markGray_List(this->next_Cons);
    break;
  case Nil_tag:
    break;
  }
}
void $scan_List(struct List* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_List(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Cons_tag:
    $scan_List(this->next_Cons);
    break;
  case Nil_tag:
    break;
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Cons_tag:
      this->next_Cons->rc ++;
      $scanBlack_List(this->next_Cons);
      break;
    case Nil_tag:
      break;
    }
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Cons_tag:
      $collectWhite_List(this->next_Cons);
      break;
    case Nil_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_List;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case Cons_tag:
    printf("Cons {");
    printf("next=");
    $print_List(this->next_Cons);
    printf(", ");
    printf("}");
    break;
  case Nil_tag:
    printf("Nil {");
    printf("}");
    break;
  }
}
struct List* new$Cons(struct List* next) {
  struct List* $res = malloc(sizeof (struct List));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_List;
  $res->kind = Cons_tag;
  $res->next_Cons = next;
  $res->next_Cons->rc ++;
  return $res;
}
struct List* new$Nil() {
  struct List* $res = malloc(sizeof (struct List));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_List;
  $res->kind = Nil_tag;
  return $res;
}
int fn$f(struct List* list) {
  list->rc ++;
  struct List* matchobj$0 = list;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Cons_tag:
    struct List* list$0 = matchobj$0->next_Cons;
    list$0->rc ++;
    $decr_List(list$0);
    struct List* list$1 = new$Nil();
    list$1->rc ++;
    $decr_List(list$1);
    matchres$1 = 0;
    break;
  case Nil_tag:
    matchres$1 = 2;
    break;
  }
  $decr_List(list);
  int ret$2 = matchres$1;
  return ret$2;
}
int main() {
  pcrBuckets = calloc(sizeof(void *), 1);
  numSccs = 1;
  fn$f(new$Cons(new$Nil()));
  int ret$3 = 0;
  processAllPCRs();
  free(pcrBuckets);
  return ret$3;
}

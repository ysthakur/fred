#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };

struct PCR {
  void *obj;
  int scc;
  void (*markGray)(void *);
  void (*scan)(void *);
  void (*collectWhite)(void *);
  struct PCR *next;
};

struct FreeCell {
  int rc;
  enum Color color;
  struct FreeCell *next;
  void (*free)(void *);
};

struct PCR *pcrs;
struct FreeCell *freeList = NULL;

void addPCR(
    void *obj,
    int scc,
    void (*markGray)(void *),
    void (*scan)(void *),
    void (*collectWhite)(void *)
) {
  fprintf(stderr, "[addPCR] scc: %d\n", scc);
  struct PCR **prev = &pcrs;
  while (*prev != NULL && (*prev)->scc <= scc) {
    if ((*prev)->obj == obj) return;
    fprintf(stderr, "[addPCR] prev scc: %d\n", (*prev)->scc);
    prev = &(*prev)->next;
  }
  struct PCR *pcr = malloc(sizeof(struct PCR));
  pcr->obj = obj;
  pcr->scc = scc;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = (*prev == NULL) ? NULL : (*prev)->next;
  *prev = pcr;
}

void removePCR(void *obj) {
  struct PCR *head = pcrs;
  struct PCR **prev = &pcrs;
  while (head != NULL) {
    if (head->obj == obj) {
      *prev = head->next;
      free(head);
      head = *prev;
      break;
    } else {
      prev = &head->next;
      head = head->next;
    }
  }
}

void markGrayAllPCRs(struct PCR *head, int scc) {
  if (head == NULL || head->scc != scc) return;
  struct PCR *next = head->next;
  head->markGray(head->obj);
  markGrayAllPCRs(next, scc);
}

void scanAllPCRs(struct PCR *head, int scc) {
  if (head == NULL || head->scc != scc) return;
  struct PCR *next = head->next;
  head->scan(head->obj);
  scanAllPCRs(next, scc);
}

void collectWhiteAllPCRs(struct PCR *head, int scc) {
  if (head == NULL || head->scc != scc) return;
  struct PCR *next = head->next;
  head->collectWhite(head->obj);
  free(head);
  pcrs = next;
  collectWhiteAllPCRs(next, scc);
}

void collectFreeList() {
  while (freeList != NULL) {
    struct FreeCell *next = freeList->next;
    (freeList->free)(freeList);
    freeList = next;
  }
}

void processAllPCRs() {
  if (pcrs == NULL) return;
  int firstScc = pcrs->scc;
  markGrayAllPCRs(pcrs, firstScc);
  scanAllPCRs(pcrs, firstScc);
  if (freeList != NULL) {
    fprintf(stderr, "Free list should be null\n");
    exit(1);
  }
  collectWhiteAllPCRs(pcrs, firstScc);
  collectFreeList();
  fprintf(stderr, "firstScc: %d\n", firstScc);
  if (pcrs != NULL) {
    processAllPCRs();
  }
}
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Color color;
  void (*print)();
  enum Foo_kind kind;
  char* common;
  union {
    struct { struct Foo* foo_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
char* fn$foo(struct Foo* param);
void $free_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      $decr_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      0,
      (void *) $markGray_Foo,
      (void *) $scan_Foo,
      (void *) $collectWhite_Foo);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Bar_tag:
    this->foo_Bar->rc --;
    $markGray_Foo(this->foo_Bar);
    break;
  case Baz_tag:
    break;
  }
}
void $scan_Foo(struct Foo* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Foo(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Bar_tag:
    $scan_Foo(this->foo_Bar);
    break;
  case Baz_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      this->foo_Bar->rc ++;
      $scanBlack_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Bar_tag:
      $collectWhite_Foo(this->foo_Bar);
      break;
    case Baz_tag:
      break;
    }
    fprintf(stderr, "Removing Foo\n");
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    printf("Bar {");
    printf("foo=");
    $print_Foo(this->foo_Bar);
    printf(", ");
    printf("common=");
    printf("%s", this->common);
    printf(", ");
    printf("notcommon=");
    printf("%s", this->notcommon_Bar);
    printf(", ");
    printf("}");
    break;
  case Baz_tag:
    printf("Baz {");
    printf("blech=");
    printf("%s", this->blech_Baz);
    printf(", ");
    printf("gah=");
    printf("%d", this->gah_Baz);
    printf(", ");
    printf("common=");
    printf("%s", this->common);
    printf(", ");
    printf("notcommon=");
    printf("%d", this->notcommon_Baz);
    printf(", ");
    printf("}");
    break;
  }
}
char* fn$foo(struct Foo* param) {
  param->rc ++;
  char* ret$0 = param->common;
  $decr_Foo(param);
  return ret$0;
}

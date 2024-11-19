#include <stdlib.h>
#include <stdio.h>

enum Color { kBlack, kGray, kWhite };

struct PCR {
  void *obj;
  void (*markGray)(void *);
  void (*scan)(void *);
  void (*collectWhite)(void *);
  struct PCR *next;
};

struct PCRBucket {
  int scc;
  struct PCR *first;
  struct PCR *last;
  struct PCRBucket *next;
};

// Common object header
typedef struct {
  int rc;
  enum Color color;
  int addedPCR;
  int kind;
} Common;
struct FreeCell {
  Common *obj;
  struct FreeCell *next;
  void (*free)(void *);
};

struct PCRBucket *pcrBuckets = NULL;
struct FreeCell *freeList = NULL;

void addPCR(
    Common *obj,
    int scc,
    void (*markGray)(void *),
    void (*scan)(void *),
    void (*collectWhite)(void *)
) {
  if (obj->addedPCR) return;
  obj->addedPCR = 1;

  struct PCRBucket **prev = &pcrBuckets;
  while (*prev != NULL && (*prev)->scc < scc) {
    // fprintf(stderr, "[addPCR] prev scc: %d\n", (*prev)->scc);
    prev = &(*prev)->next;
  }

  struct PCR *pcr = malloc(sizeof(struct PCR));
  fprintf(stderr, "[addPCR] Added PCR %p, prev = %p, scc: %d\n", pcr, *prev, scc);
  pcr->obj = obj;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = NULL;

  if (*prev == NULL || scc < (*prev)->scc) {
    struct PCRBucket *newBucket = malloc(sizeof(struct PCRBucket));
    newBucket->scc = scc;
    newBucket->first = pcr;
    newBucket->last = pcr;
    newBucket->next = *prev;
    *prev = newBucket;
  } else {
    (*prev)->last->next = pcr;
    (*prev)->last = pcr;
  }
}

void removePCR(Common *obj, int scc) {
  if (!obj->addedPCR) return;
  obj->addedPCR = 0;
  fprintf(stderr, "[removePCR] Trying to remove %p\n", obj);

  struct PCRBucket *bucket = pcrBuckets;
  while (bucket->scc != scc) {
    bucket = bucket->next;
  }

  struct PCR *head = bucket->first;
  struct PCR **prev = &bucket->first;
  while (head != NULL) {
    fprintf(stderr, "[removePCR] head = %p\n", head);
    if (head->obj == obj) {
      fprintf(stderr, "[removePCR] Removed %p\n", head);
      struct PCR *next = head->next;
      free(head);
      *prev = next;
      break;
    } else {
      prev = &head->next;
      head = head->next;
    }
  }
}

void markGrayAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->markGray(head->obj);
  markGrayAllPCRs(next);
}

void scanAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->scan(head->obj);
  scanAllPCRs(next);
}

void collectWhiteAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->collectWhite(head->obj);
  free(head);
  collectWhiteAllPCRs(next);
}

void collectFreeList() {
  while (freeList != NULL) {
    struct FreeCell *next = freeList->next;
    (freeList->free)(freeList->obj);
    free(freeList);
    freeList = next;
  }
}

void processAllPCRs() {
  while (pcrBuckets != NULL) {
    markGrayAllPCRs(pcrBuckets->first);
    scanAllPCRs(pcrBuckets->first);
    if (freeList != NULL) {
      fprintf(stderr, "Free list should be null\n");
      exit(1);
    }
    collectWhiteAllPCRs(pcrBuckets->first);
    collectFreeList();
    fprintf(stderr, "[processAllPCRs]: Processed scc %d\n", pcrBuckets->scc);
    struct PCRBucket *next = pcrBuckets->next;
    free(pcrBuckets);
    pcrBuckets = next;
  }
}
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  union {
    struct { int x_Bar; int y_Bar; };
    struct { char* a_Baz; int b_Baz; };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
int fn$foo(struct Foo* foo);
int main();
void $free_Foo(struct Foo* this) {
  fprintf(stderr, "Freeing Foo\n");
  switch (this->kind) {
  case Bar_tag:
    break;
  case Baz_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  fprintf(stderr, "Decrementing Foo (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Bar_tag:
      break;
    case Baz_tag:
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Bar_tag:
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
      break;
    case Baz_tag:
      break;
    }
    fprintf(stderr, "Removing Foo\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Foo;
  }
}
void $print_Foo(struct Foo* this) {
  switch (this->kind) {
  case Bar_tag:
    printf("Bar {");
    printf("x=");
    printf("%d", this->x_Bar);
    printf(", ");
    printf("y=");
    printf("%d", this->y_Bar);
    printf(", ");
    printf("}");
    break;
  case Baz_tag:
    printf("Baz {");
    printf("a=");
    printf("%s", this->a_Baz);
    printf(", ");
    printf("b=");
    printf("%d", this->b_Baz);
    printf(", ");
    printf("}");
    break;
  }
}
int fn$foo(struct Foo* foo) {
  foo->rc ++;
  struct Foo* matchobj$0 = foo;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Bar_tag:
    int x = matchobj$0->x_Bar;
    int yyy = matchobj$0->y_Bar;
    matchres$1 = x + yyy;
    break;
  case Baz_tag:
    char* astr = matchobj$0->a_Baz;
    int b = matchobj$0->b_Baz;
    matchres$1 = b;
    break;
  }
  int ret$2 = matchres$1;
  $decr_Foo(foo);
  return ret$2;
}
int main() {
  struct Foo* ctorres$3 = malloc(sizeof (struct Foo));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
  ctorres$3->addedPCR = 0;
  ctorres$3->print = $print_Foo;
  ctorres$3->kind = Bar_tag;
  ctorres$3->x_Bar = 1;
  ctorres$3->y_Bar = 2;
  struct Foo* foo = ctorres$3;
  foo->rc ++;
  int ret$4 = fn$foo(foo);
  $decr_Foo(foo);
  processAllPCRs();
  return ret$4;
}

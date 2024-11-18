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
enum List_kind { Nil_tag, Cons_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { int value_Cons; struct List* next_Cons; };
  };
};
void $free_List(struct List* this);
void $decr_List(struct List* this);
void $markGray_List(struct List* this);
void $scan_List(struct List* this);
void $scanBlack_List(struct List* this);
void $collectWhite_List(struct List* this);
void $print_List(struct List* this);
int fn$sum(struct List* list);
int main();
void $free_List(struct List* this) {
  fprintf(stderr, "Freeing List\n");
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    break;
  }
  free(this);
}
void $decr_List(struct List* this) {
  fprintf(stderr, "Decrementing List (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $decr_List(this->next_Cons);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_List,
      (void *) $scan_List,
      (void *) $collectWhite_List);
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Nil_tag:
    break;
  case Cons_tag:
    this->next_Cons->rc --;
    $markGray_List(this->next_Cons);
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
  case Nil_tag:
    break;
  case Cons_tag:
    $scan_List(this->next_Cons);
    break;
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      this->next_Cons->rc ++;
      $scanBlack_List(this->next_Cons);
      break;
    }
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Nil_tag:
      break;
    case Cons_tag:
      $collectWhite_List(this->next_Cons);
      break;
    }
    fprintf(stderr, "Removing List\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_List;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case Nil_tag:
    printf("Nil {");
    printf("}");
    break;
  case Cons_tag:
    printf("Cons {");
    printf("value=");
    printf("%d", this->value_Cons);
    printf(", ");
    printf("next=");
    $print_List(this->next_Cons);
    printf(", ");
    printf("}");
    break;
  }
}
int fn$sum(struct List* list) {
  list->rc ++;
  struct List* matchobj$0 = list;
  int matchres$1;
  switch (matchobj$0->kind) {
  case Nil_tag:
    matchres$1 = 0;
    break;
  case Cons_tag:
    int value = matchobj$0->value_Cons;
    struct List* tail = matchobj$0->next_Cons;
    tail->rc ++;
    matchres$1 = value + fn$sum(tail);
    $decr_List(tail);
    break;
  }
  int ret$2 = matchres$1;
  $decr_List(list);
  return ret$2;
}
int main() {
  struct List* ctorres$3 = malloc(sizeof (struct List));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
  ctorres$3->addedPCR = 0;
  ctorres$3->print = $print_List;
  ctorres$3->kind = Cons_tag;
  ctorres$3->value_Cons = 1;
  struct List* ctorres$4 = malloc(sizeof (struct List));
  ctorres$4->rc = 0;
  ctorres$4->color = kBlack;
  ctorres$4->addedPCR = 0;
  ctorres$4->print = $print_List;
  ctorres$4->kind = Cons_tag;
  ctorres$4->value_Cons = 2;
  struct List* ctorres$5 = malloc(sizeof (struct List));
  ctorres$5->rc = 0;
  ctorres$5->color = kBlack;
  ctorres$5->addedPCR = 0;
  ctorres$5->print = $print_List;
  ctorres$5->kind = Cons_tag;
  ctorres$5->value_Cons = 4;
  struct List* ctorres$6 = malloc(sizeof (struct List));
  ctorres$6->rc = 0;
  ctorres$6->color = kBlack;
  ctorres$6->addedPCR = 0;
  ctorres$6->print = $print_List;
  ctorres$6->kind = Nil_tag;
  ctorres$5->next_Cons = ctorres$6;
  ctorres$5->next_Cons->rc ++;
  ctorres$4->next_Cons = ctorres$5;
  ctorres$4->next_Cons->rc ++;
  ctorres$3->next_Cons = ctorres$4;
  ctorres$3->next_Cons->rc ++;
  struct List* list = ctorres$3;
  list->rc ++;
  printf("%d\n", fn$sum(list));
  int ret$7 = 0;
  $decr_List(list);
  processAllPCRs();
  return ret$7;
}

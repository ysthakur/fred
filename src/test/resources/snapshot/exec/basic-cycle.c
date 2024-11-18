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

struct PCR *pcrs;
struct FreeCell *freeList = NULL;

void printPCRs() {
  fprintf(stderr, "[printPCRs] pcrs: ");
  for (struct PCR *head = pcrs; head != NULL; head = head->next) {
    fprintf(stderr, "%p, ", head);
  }
  fprintf(stderr, "\n");
}

void addPCR(
    Common *obj,
    int scc,
    void (*markGray)(void *),
    void (*scan)(void *),
    void (*collectWhite)(void *)
) {
  if (obj->addedPCR) return;
  obj->addedPCR = 1;
  struct PCR **prev = &pcrs;
  while (*prev != NULL && (*prev)->scc <= scc) {
    // fprintf(stderr, "[addPCR] prev scc: %d\n", (*prev)->scc);
    prev = &(*prev)->next;
  }
  struct PCR *pcr = malloc(sizeof(struct PCR));
  fprintf(stderr, "[addPCR] Added PCR %p, prev = %p, scc: %d\n", pcr, *prev, scc);
  pcr->obj = obj;
  pcr->scc = scc;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = *prev;
  *prev = pcr;
  printPCRs();
}

void removePCR(Common *obj) {
  if (!obj->addedPCR) return;
  obj->addedPCR = 0;
  struct PCR *head = pcrs;
  struct PCR **prev = &pcrs;
  fprintf(stderr, "[removePCR] Trying to remove %p\n", obj);
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

void collectWhiteAllPCRs(int scc) {
  if (pcrs == NULL || pcrs->scc != scc) return;
  fprintf(stderr, "[collectWhiteAllPCRs] pcr: %p, scc: %d\n", pcrs, scc);
  printPCRs();
  struct PCR *next = pcrs->next;
  pcrs->collectWhite(pcrs->obj);
  free(pcrs);
  fprintf(stderr, "Removed a PCR %p\n", pcrs);
  pcrs = next;
  collectWhiteAllPCRs(scc);
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
  if (pcrs == NULL) return;
  int firstScc = pcrs->scc;
  markGrayAllPCRs(pcrs, firstScc);
  scanAllPCRs(pcrs, firstScc);
  if (freeList != NULL) {
    fprintf(stderr, "Free list should be null\n");
    exit(1);
  }
  collectWhiteAllPCRs(firstScc);
  collectFreeList();
  fprintf(stderr, "firstScc: %d\n", firstScc);
  if (pcrs != NULL) {
    processAllPCRs();
  }
}
enum Option_kind { None_tag, Some_tag };
struct Option {
  int rc;
  enum Color color;
  int addedPCR;
  enum Option_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { struct List* value_Some; };
  };
};
enum List_kind { List_tag };
struct List {
  int rc;
  enum Color color;
  int addedPCR;
  enum List_kind kind;
  void (*print)();
  int value;
  struct Option* next;
  union {
    struct {  };
  };
};
void $free_Option(struct Option* this);
void $free_List(struct List* this);
void $decr_Option(struct Option* this);
void $decr_List(struct List* this);
void $markGray_Option(struct Option* this);
void $markGray_List(struct List* this);
void $scan_Option(struct Option* this);
void $scan_List(struct List* this);
void $scanBlack_Option(struct Option* this);
void $scanBlack_List(struct List* this);
void $collectWhite_Option(struct Option* this);
void $collectWhite_List(struct List* this);
void $print_Option(struct Option* this);
void $print_List(struct List* this);
int main();
void $free_Option(struct Option* this) {
  fprintf(stderr, "Freeing Option\n");
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    break;
  }
  free(this);
}
void $free_List(struct List* this) {
  fprintf(stderr, "Freeing List\n");
  switch (this->kind) {
  case List_tag:
    break;
  }
  free(this);
}
void $decr_Option(struct Option* this) {
  fprintf(stderr, "Decrementing Option (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      $decr_List(this->value_Some);
      break;
    }
    removePCR((void *) this);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_Option,
      (void *) $scan_Option,
      (void *) $collectWhite_Option);
  }
}
void $decr_List(struct List* this) {
  fprintf(stderr, "Decrementing List (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case List_tag:
      $decr_Option(this->next);
      break;
    }
    removePCR((void *) this);
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
void $markGray_Option(struct Option* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    this->value_Some->rc --;
    $markGray_List(this->value_Some);
    break;
  }
}
void $markGray_List(struct List* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case List_tag:
    this->next->rc --;
    $markGray_Option(this->next);
    break;
  }
}
void $scan_Option(struct Option* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Option(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case None_tag:
    break;
  case Some_tag:
    $scan_List(this->value_Some);
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
  case List_tag:
    $scan_Option(this->next);
    break;
  }
}
void $scanBlack_Option(struct Option* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      this->value_Some->rc ++;
      $scanBlack_List(this->value_Some);
      break;
    }
  }
}
void $scanBlack_List(struct List* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case List_tag:
      this->next->rc ++;
      $scanBlack_Option(this->next);
      break;
    }
  }
}
void $collectWhite_Option(struct Option* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      $collectWhite_List(this->value_Some);
      break;
    }
    fprintf(stderr, "Removing Option\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Option;
  }
}
void $collectWhite_List(struct List* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case List_tag:
      $collectWhite_Option(this->next);
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
void $print_Option(struct Option* this) {
  switch (this->kind) {
  case None_tag:
    printf("None {");
    printf("}");
    break;
  case Some_tag:
    printf("Some {");
    printf("value=");
    $print_List(this->value_Some);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_List(struct List* this) {
  switch (this->kind) {
  case List_tag:
    printf("List {");
    printf("value=");
    printf("%d", this->value);
    printf(", ");
    printf("next=");
    $print_Option(this->next);
    printf(", ");
    printf("}");
    break;
  }
}
int main() {
  struct List* ctorres$0 = malloc(sizeof (struct List));
  ctorres$0->rc = 0;
  ctorres$0->color = kBlack;
  ctorres$0->addedPCR = 0;
  ctorres$0->print = $print_List;
  ctorres$0->kind = List_tag;
  ctorres$0->value = 1;
  struct Option* ctorres$1 = malloc(sizeof (struct Option));
  ctorres$1->rc = 0;
  ctorres$1->color = kBlack;
  ctorres$1->addedPCR = 0;
  ctorres$1->print = $print_Option;
  ctorres$1->kind = None_tag;
  ctorres$0->next = ctorres$1;
  ctorres$0->next->rc ++;
  struct List* a = ctorres$0;
  a->rc ++;
  struct List* ctorres$2 = malloc(sizeof (struct List));
  ctorres$2->rc = 0;
  ctorres$2->color = kBlack;
  ctorres$2->addedPCR = 0;
  ctorres$2->print = $print_List;
  ctorres$2->kind = List_tag;
  ctorres$2->value = 2;
  struct Option* ctorres$3 = malloc(sizeof (struct Option));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
  ctorres$3->addedPCR = 0;
  ctorres$3->print = $print_Option;
  ctorres$3->kind = Some_tag;
  ctorres$3->value_Some = a;
  ctorres$3->value_Some->rc ++;
  ctorres$2->next = ctorres$3;
  ctorres$2->next->rc ++;
  struct List* b = ctorres$2;
  b->rc ++;
  struct Option* ctorres$4 = malloc(sizeof (struct Option));
  ctorres$4->rc = 0;
  ctorres$4->color = kBlack;
  ctorres$4->addedPCR = 0;
  ctorres$4->print = $print_Option;
  ctorres$4->kind = Some_tag;
  ctorres$4->value_Some = b;
  ctorres$4->value_Some->rc ++;
  $decr_Option(a->next);
  a->next = ctorres$4;
  a->next->rc ++;
  a->next;
  printf("%d\n", a->value + b->value);
  int ret$5 = 0;
  $decr_List(b);
  $decr_List(a);
  processAllPCRs();
  return ret$5;
}

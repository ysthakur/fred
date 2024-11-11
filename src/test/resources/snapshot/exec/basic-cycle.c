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
  printf("[addPCR] scc: %d\n", scc);
  struct PCR **prev = &pcrs;
  while (*prev != NULL && (*prev)->scc <= scc) {
    if ((*prev)->obj == obj) return;
    printf("[addPCR] prev scc: %d\n", (*prev)->scc);
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
    free(freeList);
    freeList = next;
  }
}

void processAllPCRs() {
  markGrayAllPCRs(pcrs);
  scanAllPCRs(pcrs);
  freeList = NULL;
  collectWhiteAllPCRs(pcrs);
  collectFreeList();
}
enum Option_kind { None_tag, Some_tag };
struct Option {
  int rc;
  enum Color color;
  enum Option_kind kind;
  union {
    struct {  };
    struct { struct List* value_Some; };
  };
};
enum List_kind { List_tag };
struct List {
  int rc;
  enum Color color;
  enum List_kind kind;
  int value;
  struct Option* next;
  union {
    struct {  };
  };
};
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
int main();
void $decr_Option(struct Option* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case None_tag:
      break;
    case Some_tag:
      $decr_List(this->value_Some);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      0,
      (void *) $markGray_Option,
      (void *) $scan_Option,
      (void *) $collectWhite_Option);
  }
}
void $decr_List(struct List* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case List_tag:
      $decr_Option(this->next);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
  }
}
int main() {
  struct List* ctorres$0 = malloc(sizeof (struct List));
  ctorres$0->rc = 0;
  ctorres$0->color = kBlack;
  ctorres$0->kind = List_tag;
  ctorres$0->value = 1;
  struct Option* ctorres$1 = malloc(sizeof (struct Option));
  ctorres$1->rc = 0;
  ctorres$1->color = kBlack;
  ctorres$1->kind = None_tag;
  ctorres$0->next = ctorres$1;
  ctorres$0->next->rc ++;
  struct List* a = ctorres$0;
  a->rc ++;
  struct List* ctorres$2 = malloc(sizeof (struct List));
  ctorres$2->rc = 0;
  ctorres$2->color = kBlack;
  ctorres$2->kind = List_tag;
  ctorres$2->value = 2;
  struct Option* ctorres$3 = malloc(sizeof (struct Option));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
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

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

struct FreeCell {
  int rc;
  enum Color color;
  struct FreeCell *next;
};

struct PCR *pcrs;
struct FreeCell *freeList = NULL;

void addPCR(
    void *obj,
    void (*markGray)(void *),
    void (*scan)(void *),
    void (*collectWhite)(void *)
) {
  for (struct PCR* head = pcrs; head != NULL; head = head->next) {
    if (head->obj == obj) return;
  }
  struct PCR *pcr = malloc(sizeof(struct PCR));
  pcr->obj = obj;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = pcrs;
  pcrs = pcr;
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
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  int scc;
  enum Color color;
  enum Foo_kind kind;
  int field;
  union {
    struct {  };
  };
};
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $decr_Foo(struct Foo* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_Foo,
      (void *) $scan_Foo,
      (void *) $collectWhite_Foo);
  }
}
void $markGray_Foo(struct Foo* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Foo_tag:
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
  case Foo_tag:
    break;
  }
}
void $scanBlack_Foo(struct Foo* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
  }
}
void $collectWhite_Foo(struct Foo* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
  }
}
int fn$bar(struct Foo* f) {
  f->rc ++;
  f->field = 5;
  f->field = 6;
  f->field;
  int ret$0 = f->field;
  $decr_Foo(f);
  return ret$0;
}

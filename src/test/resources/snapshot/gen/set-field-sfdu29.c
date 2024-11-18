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
enum Foo_kind { Foo_tag };
struct Foo {
  int rc;
  enum Color color;
  int addedPCR;
  enum Foo_kind kind;
  void (*print)();
  int field;
  union {
    struct {  };
  };
};
void $free_Foo(struct Foo* this);
void $decr_Foo(struct Foo* this);
void $markGray_Foo(struct Foo* this);
void $scan_Foo(struct Foo* this);
void $scanBlack_Foo(struct Foo* this);
void $collectWhite_Foo(struct Foo* this);
void $print_Foo(struct Foo* this);
int fn$bar(struct Foo* f);
void $free_Foo(struct Foo* this) {
  fprintf(stderr, "Freeing Foo\n");
  switch (this->kind) {
  case Foo_tag:
    break;
  }
  free(this);
}
void $decr_Foo(struct Foo* this) {
  fprintf(stderr, "Decrementing Foo (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Foo_tag:
      break;
    }
    removePCR((void *) this);
    free(this);
  } else {
    addPCR(
      (void *) this,
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
  case Foo_tag:
    printf("Foo {");
    printf("field=");
    printf("%d", this->field);
    printf(", ");
    printf("}");
    break;
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

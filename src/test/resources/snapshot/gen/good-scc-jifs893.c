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

/*
 * Types and functions for doing reference counting stuff
 */

#include <stdlib.h>
#include <stdio.h>

enum Color
{
  kBlack,
  kGray,
  kWhite
};

struct PCR
{
  void *obj;
  void (*markGray)(void *);
  void (*scan)(void *);
  void (*collectWhite)(void *);
  struct PCR *next;
};

struct PCRBucket
{
  int scc;
  struct PCR *first;
  struct PCR *last;
  struct PCRBucket *next;
};

// Common object header
typedef struct
{
  int rc;
  enum Color color;
  int addedPCR;
  int kind;
} Common;

struct FreeCell
{
  Common *obj;
  struct FreeCell *next;
  void (*free)(void *);
};

struct PCRBucket *pcrBuckets = NULL;
struct FreeCell *freeList = NULL;

/** For dropping objects that never get assigned anywhere */
void drop(Common *obj, void (*decrRC)(void *)) {
  obj->rc ++;
  decrRC(obj);
}

void addPCR(
    Common *obj,
    int scc,
    void (*markGray)(void *),
    void (*scan)(void *),
    void (*collectWhite)(void *))
{
  if (obj->addedPCR)
    return;
  obj->addedPCR = 1;

  struct PCRBucket **prev = &pcrBuckets;
  while (*prev != NULL && (*prev)->scc < scc)
  {
    // fprintf(stderr, "[addPCR] prev scc: %d\n", (*prev)->scc);
    prev = &(*prev)->next;
  }

  struct PCR *pcr = malloc(sizeof(struct PCR));
  fprintf(stderr, "[addPCR] Added PCR %p, prev = %p, scc: %d, obj: %p\n", pcr, *prev, scc, obj);
  pcr->obj = obj;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = NULL;

  if (*prev == NULL || scc < (*prev)->scc)
  {
    struct PCRBucket *newBucket = malloc(sizeof(struct PCRBucket));
    newBucket->scc = scc;
    newBucket->first = pcr;
    newBucket->last = pcr;
    newBucket->next = *prev;
    *prev = newBucket;
  }
  else
  {
    (*prev)->last->next = pcr;
    (*prev)->last = pcr;
  }
}

void removePCR(Common *obj, int scc)
{
  if (!obj->addedPCR)
    return;
  obj->addedPCR = 0;
  fprintf(stderr, "[removePCR] Trying to remove %p\n", obj);

  struct PCRBucket **prevBucket = &pcrBuckets;
  struct PCRBucket *bucket = pcrBuckets;
  while (bucket->scc != scc)
  {
    prevBucket = &bucket->next;
    bucket = bucket->next;
  }

  if (bucket->first->obj == obj) {
    if (bucket->last->obj == obj) {
      // This was the only PCR in the bucket, so get rid of the bucket too
      free(bucket->first);
      *prevBucket = bucket->next;
      free(bucket);
      return;
    } else {
      bucket->first = bucket->first->next;
      free(bucket->first);
      return;
    }
  }

  struct PCR *prev = bucket->first;
  struct PCR *head = prev->next;
  while (head != NULL)
  {
    fprintf(stderr, "[removePCR] head = %p\n", head);
    if (head->obj == obj)
    {
      fprintf(stderr, "[removePCR] Removed %p (obj=%p)\n", head, obj);
      prev->next = head->next;
      if (head == bucket->last) {
        bucket->last = prev;
      }
      free(head);
      break;
    }
    else
    {
      prev = head;
      head = head->next;
    }
  }
}

void markGrayAllPCRs(struct PCR *head)
{
  if (head == NULL)
    return;
  struct PCR *next = head->next;
  head->markGray(head->obj);
  markGrayAllPCRs(next);
}

void scanAllPCRs(struct PCR *head)
{
  if (head == NULL)
    return;
  struct PCR *next = head->next;
  head->scan(head->obj);
  scanAllPCRs(next);
}

void collectWhiteAllPCRs(struct PCR *head)
{
  if (head == NULL)
    return;
  struct PCR *next = head->next;
  head->collectWhite(head->obj);
  free(head);
  collectWhiteAllPCRs(next);
}

void collectFreeList()
{
  while (freeList != NULL)
  {
    struct FreeCell *next = freeList->next;
    (freeList->free)(freeList->obj);
    free(freeList);
    freeList = next;
  }
}

void processAllPCRs()
{
  while (pcrBuckets != NULL)
  {
    markGrayAllPCRs(pcrBuckets->first);
    scanAllPCRs(pcrBuckets->first);
    if (freeList != NULL)
    {
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

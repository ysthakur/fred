/*
 * Types and functions for doing reference counting stuff
 */

#include <stdlib.h>
#include <stdio.h>
#include <time.h>

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

struct PCR **pcrBuckets = NULL;
int numSccs = 0;
struct FreeCell *freeList = NULL;

/** For dropping objects that never get assigned anywhere */
void drop(Common *obj, void (*decrRC)(void *))
{
  obj->rc++;
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

  struct PCR *pcr = malloc(sizeof(struct PCR));
  pcr->obj = obj;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = NULL;

  pcr->next = pcrBuckets[scc];
  pcrBuckets[scc] = pcr;
}

void removePCR(Common *obj, int scc)
{
  if (!obj->addedPCR)
    return;
  obj->addedPCR = 0;

  struct PCR **prev = &pcrBuckets[scc];
  struct PCR *head = pcrBuckets[scc];
  while (head != NULL)
  {
    if (head->obj == obj)
    {
      *prev = head->next;
      free(head);
      return;
    }
    else
    {
      prev = &head->next;
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
  for (int scc = 0; scc < numSccs; scc++)
  {
    for (struct PCR *curr = pcrBuckets[scc]; curr != NULL; curr = curr->next)
    {
      curr->markGray(curr->obj);
    }
    for (struct PCR *curr = pcrBuckets[scc]; curr != NULL; curr = curr->next)
    {
      curr->scan(curr->obj);
    }
    if (freeList != NULL)
    {
      fprintf(stderr, "Free list should be null\n");
      exit(1);
    }
    struct PCR *curr = pcrBuckets[scc];
    while (curr != NULL)
    {
      curr->collectWhite(curr->obj);
      struct PCR *next = curr->next;
      free(curr);
      curr = next;
    }
    pcrBuckets[scc] = NULL;
    collectFreeList();
  }
}

// From https://stackoverflow.com/a/14783909/11882002
static inline u_int64_t rdtscp()
{
  u_int64_t rax, rdx;
  u_int32_t aux;
  asm volatile("rdtscp\n" : "=a"(rax), "=d"(rdx), "=c"(aux) : :);
  return (rdx << 32) + rax;
}

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
enum CtxRef_kind { CtxRef_tag };
struct CtxRef {
  int rc;
  enum Color color;
  int addedPCR;
  enum CtxRef_kind kind;
  void (*print)();
  struct Context* ref;
  union {
    struct {  };
  };
};
enum Context_kind { Context_tag };
struct Context {
  int rc;
  enum Color color;
  int addedPCR;
  enum Context_kind kind;
  void (*print)();
  char* name;
  struct FileList* files;
  union {
    struct {  };
  };
};
enum FileList_kind { FileNil_tag, FileCons_tag };
struct FileList {
  int rc;
  enum Color color;
  int addedPCR;
  enum FileList_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { struct Context* ctx_FileCons; struct File* head_FileCons; struct FileList* tail_FileCons; };
  };
};
enum File_kind { File_tag };
struct File {
  int rc;
  enum Color color;
  int addedPCR;
  enum File_kind kind;
  void (*print)();
  struct ExprList* exprs;
  union {
    struct {  };
  };
};
enum ExprList_kind { ExprNil_tag, ExprCons_tag };
struct ExprList {
  int rc;
  enum Color color;
  int addedPCR;
  enum ExprList_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { struct Expr* head_ExprCons; struct ExprList* tail_ExprCons; };
  };
};
enum Expr_kind { Expr_tag };
struct Expr {
  int rc;
  enum Color color;
  int addedPCR;
  enum Expr_kind kind;
  void (*print)();
  struct File* file;
  union {
    struct {  };
  };
};
void $free_CtxRef(struct CtxRef* this);
void $free_Context(struct Context* this);
void $free_FileList(struct FileList* this);
void $free_File(struct File* this);
void $free_ExprList(struct ExprList* this);
void $free_Expr(struct Expr* this);
void $decr_CtxRef(struct CtxRef* this);
void $decr_Context(struct Context* this);
void $decr_FileList(struct FileList* this);
void $decr_File(struct File* this);
void $decr_ExprList(struct ExprList* this);
void $decr_Expr(struct Expr* this);
void $markGray_CtxRef(struct CtxRef* this);
void $markGray_Context(struct Context* this);
void $markGray_FileList(struct FileList* this);
void $markGray_File(struct File* this);
void $markGray_ExprList(struct ExprList* this);
void $markGray_Expr(struct Expr* this);
void $scan_CtxRef(struct CtxRef* this);
void $scan_Context(struct Context* this);
void $scan_FileList(struct FileList* this);
void $scan_File(struct File* this);
void $scan_ExprList(struct ExprList* this);
void $scan_Expr(struct Expr* this);
void $scanBlack_CtxRef(struct CtxRef* this);
void $scanBlack_Context(struct Context* this);
void $scanBlack_FileList(struct FileList* this);
void $scanBlack_File(struct File* this);
void $scanBlack_ExprList(struct ExprList* this);
void $scanBlack_Expr(struct Expr* this);
void $collectWhite_CtxRef(struct CtxRef* this);
void $collectWhite_Context(struct Context* this);
void $collectWhite_FileList(struct FileList* this);
void $collectWhite_File(struct File* this);
void $collectWhite_ExprList(struct ExprList* this);
void $collectWhite_Expr(struct Expr* this);
void $print_CtxRef(struct CtxRef* this);
void $print_Context(struct Context* this);
void $print_FileList(struct FileList* this);
void $print_File(struct File* this);
void $print_ExprList(struct ExprList* this);
void $print_Expr(struct Expr* this);
int main();
void $free_CtxRef(struct CtxRef* this) {
  fprintf(stderr, "Freeing CtxRef\n");
  switch (this->kind) {
  case CtxRef_tag:
    $decr_Context(this->ref);
    break;
  }
  free(this);
}
void $free_Context(struct Context* this) {
  fprintf(stderr, "Freeing Context\n");
  switch (this->kind) {
  case Context_tag:
    break;
  }
  free(this);
}
void $free_FileList(struct FileList* this) {
  fprintf(stderr, "Freeing FileList\n");
  switch (this->kind) {
  case FileNil_tag:
    break;
  case FileCons_tag:
    $decr_File(this->head_FileCons);
    break;
  }
  free(this);
}
void $free_File(struct File* this) {
  fprintf(stderr, "Freeing File\n");
  switch (this->kind) {
  case File_tag:
    break;
  }
  free(this);
}
void $free_ExprList(struct ExprList* this) {
  fprintf(stderr, "Freeing ExprList\n");
  switch (this->kind) {
  case ExprNil_tag:
    break;
  case ExprCons_tag:
    break;
  }
  free(this);
}
void $free_Expr(struct Expr* this) {
  fprintf(stderr, "Freeing Expr\n");
  switch (this->kind) {
  case Expr_tag:
    break;
  }
  free(this);
}
void $decr_CtxRef(struct CtxRef* this) {
  fprintf(stderr, "Decrementing CtxRef (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case CtxRef_tag:
      $decr_Context(this->ref);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  }
}
void $decr_Context(struct Context* this) {
  fprintf(stderr, "Decrementing Context (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Context_tag:
      $decr_FileList(this->files);
      break;
    }
    removePCR((void *) this, 1);
    free(this);
  } else {
    addPCR(
      (void *) this,
      1,
      (void *) $markGray_Context,
      (void *) $scan_Context,
      (void *) $collectWhite_Context);
  }
}
void $decr_FileList(struct FileList* this) {
  fprintf(stderr, "Decrementing FileList (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case FileNil_tag:
      break;
    case FileCons_tag:
      $decr_Context(this->ctx_FileCons);
      $decr_File(this->head_FileCons);
      $decr_FileList(this->tail_FileCons);
      break;
    }
    removePCR((void *) this, 1);
    free(this);
  } else {
    addPCR(
      (void *) this,
      1,
      (void *) $markGray_FileList,
      (void *) $scan_FileList,
      (void *) $collectWhite_FileList);
  }
}
void $decr_File(struct File* this) {
  fprintf(stderr, "Decrementing File (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case File_tag:
      $decr_ExprList(this->exprs);
      break;
    }
    removePCR((void *) this, 2);
    free(this);
  } else {
    addPCR(
      (void *) this,
      2,
      (void *) $markGray_File,
      (void *) $scan_File,
      (void *) $collectWhite_File);
  }
}
void $decr_ExprList(struct ExprList* this) {
  fprintf(stderr, "Decrementing ExprList (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case ExprNil_tag:
      break;
    case ExprCons_tag:
      $decr_Expr(this->head_ExprCons);
      $decr_ExprList(this->tail_ExprCons);
      break;
    }
    removePCR((void *) this, 2);
    free(this);
  } else {
    addPCR(
      (void *) this,
      2,
      (void *) $markGray_ExprList,
      (void *) $scan_ExprList,
      (void *) $collectWhite_ExprList);
  }
}
void $decr_Expr(struct Expr* this) {
  fprintf(stderr, "Decrementing Expr (%p)\n", this);
  if (--this->rc == 0) {
    switch (this->kind) {
    case Expr_tag:
      $decr_File(this->file);
      break;
    }
    removePCR((void *) this, 2);
    free(this);
  } else {
    addPCR(
      (void *) this,
      2,
      (void *) $markGray_Expr,
      (void *) $scan_Expr,
      (void *) $collectWhite_Expr);
  }
}
void $markGray_CtxRef(struct CtxRef* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case CtxRef_tag:
    break;
  }
}
void $markGray_Context(struct Context* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Context_tag:
    this->files->rc --;
    $markGray_FileList(this->files);
    break;
  }
}
void $markGray_FileList(struct FileList* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case FileNil_tag:
    break;
  case FileCons_tag:
    this->ctx_FileCons->rc --;
    $markGray_Context(this->ctx_FileCons);
    this->tail_FileCons->rc --;
    $markGray_FileList(this->tail_FileCons);
    break;
  }
}
void $markGray_File(struct File* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case File_tag:
    this->exprs->rc --;
    $markGray_ExprList(this->exprs);
    break;
  }
}
void $markGray_ExprList(struct ExprList* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case ExprNil_tag:
    break;
  case ExprCons_tag:
    this->head_ExprCons->rc --;
    $markGray_Expr(this->head_ExprCons);
    this->tail_ExprCons->rc --;
    $markGray_ExprList(this->tail_ExprCons);
    break;
  }
}
void $markGray_Expr(struct Expr* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  switch (this->kind) {
  case Expr_tag:
    this->file->rc --;
    $markGray_File(this->file);
    break;
  }
}
void $scan_CtxRef(struct CtxRef* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_CtxRef(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case CtxRef_tag:
    break;
  }
}
void $scan_Context(struct Context* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Context(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Context_tag:
    $scan_FileList(this->files);
    break;
  }
}
void $scan_FileList(struct FileList* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_FileList(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case FileNil_tag:
    break;
  case FileCons_tag:
    $scan_Context(this->ctx_FileCons);
    $scan_FileList(this->tail_FileCons);
    break;
  }
}
void $scan_File(struct File* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_File(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case File_tag:
    $scan_ExprList(this->exprs);
    break;
  }
}
void $scan_ExprList(struct ExprList* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_ExprList(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case ExprNil_tag:
    break;
  case ExprCons_tag:
    $scan_Expr(this->head_ExprCons);
    $scan_ExprList(this->tail_ExprCons);
    break;
  }
}
void $scan_Expr(struct Expr* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Expr(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Expr_tag:
    $scan_File(this->file);
    break;
  }
}
void $scanBlack_CtxRef(struct CtxRef* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case CtxRef_tag:
      break;
    }
  }
}
void $scanBlack_Context(struct Context* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Context_tag:
      this->files->rc ++;
      $scanBlack_FileList(this->files);
      break;
    }
  }
}
void $scanBlack_FileList(struct FileList* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case FileNil_tag:
      break;
    case FileCons_tag:
      this->ctx_FileCons->rc ++;
      $scanBlack_Context(this->ctx_FileCons);
      this->tail_FileCons->rc ++;
      $scanBlack_FileList(this->tail_FileCons);
      break;
    }
  }
}
void $scanBlack_File(struct File* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case File_tag:
      this->exprs->rc ++;
      $scanBlack_ExprList(this->exprs);
      break;
    }
  }
}
void $scanBlack_ExprList(struct ExprList* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case ExprNil_tag:
      break;
    case ExprCons_tag:
      this->head_ExprCons->rc ++;
      $scanBlack_Expr(this->head_ExprCons);
      this->tail_ExprCons->rc ++;
      $scanBlack_ExprList(this->tail_ExprCons);
      break;
    }
  }
}
void $scanBlack_Expr(struct Expr* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Expr_tag:
      this->file->rc ++;
      $scanBlack_File(this->file);
      break;
    }
  }
}
void $collectWhite_CtxRef(struct CtxRef* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case CtxRef_tag:
      $collectWhite_Context(this->ref);
      break;
    }
    fprintf(stderr, "Removing CtxRef\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_CtxRef;
  }
}
void $collectWhite_Context(struct Context* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Context_tag:
      $collectWhite_FileList(this->files);
      break;
    }
    fprintf(stderr, "Removing Context\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Context;
  }
}
void $collectWhite_FileList(struct FileList* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case FileNil_tag:
      break;
    case FileCons_tag:
      $collectWhite_Context(this->ctx_FileCons);
      $collectWhite_File(this->head_FileCons);
      $collectWhite_FileList(this->tail_FileCons);
      break;
    }
    fprintf(stderr, "Removing FileList\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_FileList;
  }
}
void $collectWhite_File(struct File* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case File_tag:
      $collectWhite_ExprList(this->exprs);
      break;
    }
    fprintf(stderr, "Removing File\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_File;
  }
}
void $collectWhite_ExprList(struct ExprList* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case ExprNil_tag:
      break;
    case ExprCons_tag:
      $collectWhite_Expr(this->head_ExprCons);
      $collectWhite_ExprList(this->tail_ExprCons);
      break;
    }
    fprintf(stderr, "Removing ExprList\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_ExprList;
  }
}
void $collectWhite_Expr(struct Expr* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Expr_tag:
      $collectWhite_File(this->file);
      break;
    }
    fprintf(stderr, "Removing Expr\n");
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Expr;
  }
}
void $print_CtxRef(struct CtxRef* this) {
  switch (this->kind) {
  case CtxRef_tag:
    printf("CtxRef {");
    printf("ref=");
    $print_Context(this->ref);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_Context(struct Context* this) {
  switch (this->kind) {
  case Context_tag:
    printf("Context {");
    printf("name=");
    printf("%s", this->name);
    printf(", ");
    printf("files=");
    $print_FileList(this->files);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_FileList(struct FileList* this) {
  switch (this->kind) {
  case FileNil_tag:
    printf("FileNil {");
    printf("}");
    break;
  case FileCons_tag:
    printf("FileCons {");
    printf("ctx=");
    $print_Context(this->ctx_FileCons);
    printf(", ");
    printf("head=");
    $print_File(this->head_FileCons);
    printf(", ");
    printf("tail=");
    $print_FileList(this->tail_FileCons);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_File(struct File* this) {
  switch (this->kind) {
  case File_tag:
    printf("File {");
    printf("exprs=");
    $print_ExprList(this->exprs);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_ExprList(struct ExprList* this) {
  switch (this->kind) {
  case ExprNil_tag:
    printf("ExprNil {");
    printf("}");
    break;
  case ExprCons_tag:
    printf("ExprCons {");
    printf("head=");
    $print_Expr(this->head_ExprCons);
    printf(", ");
    printf("tail=");
    $print_ExprList(this->tail_ExprCons);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_Expr(struct Expr* this) {
  switch (this->kind) {
  case Expr_tag:
    printf("Expr {");
    printf("file=");
    $print_File(this->file);
    printf(", ");
    printf("}");
    break;
  }
}
int main() {
  struct CtxRef* ctorres$0 = malloc(sizeof (struct CtxRef));
  ctorres$0->rc = 0;
  ctorres$0->color = kBlack;
  ctorres$0->addedPCR = 0;
  ctorres$0->print = $print_CtxRef;
  ctorres$0->kind = CtxRef_tag;
  struct Context* ctorres$1 = malloc(sizeof (struct Context));
  ctorres$1->rc = 0;
  ctorres$1->color = kBlack;
  ctorres$1->addedPCR = 0;
  ctorres$1->print = $print_Context;
  ctorres$1->kind = Context_tag;
  ctorres$1->name = "foo";
  struct FileList* ctorres$2 = malloc(sizeof (struct FileList));
  ctorres$2->rc = 0;
  ctorres$2->color = kBlack;
  ctorres$2->addedPCR = 0;
  ctorres$2->print = $print_FileList;
  ctorres$2->kind = FileNil_tag;
  ctorres$1->files = ctorres$2;
  ctorres$1->files->rc ++;
  ctorres$0->ref = ctorres$1;
  ctorres$0->ref->rc ++;
  struct CtxRef* ctx = ctorres$0;
  ctx->rc ++;
  struct File* ctorres$3 = malloc(sizeof (struct File));
  ctorres$3->rc = 0;
  ctorres$3->color = kBlack;
  ctorres$3->addedPCR = 0;
  ctorres$3->print = $print_File;
  ctorres$3->kind = File_tag;
  struct ExprList* ctorres$4 = malloc(sizeof (struct ExprList));
  ctorres$4->rc = 0;
  ctorres$4->color = kBlack;
  ctorres$4->addedPCR = 0;
  ctorres$4->print = $print_ExprList;
  ctorres$4->kind = ExprNil_tag;
  ctorres$3->exprs = ctorres$4;
  ctorres$3->exprs->rc ++;
  struct File* file = ctorres$3;
  file->rc ++;
  struct Context* actualCtx = ctx->ref;
  actualCtx->rc ++;
  struct FileList* ctorres$5 = malloc(sizeof (struct FileList));
  ctorres$5->rc = 0;
  ctorres$5->color = kBlack;
  ctorres$5->addedPCR = 0;
  ctorres$5->print = $print_FileList;
  ctorres$5->kind = FileCons_tag;
  ctorres$5->ctx_FileCons = ctx->ref;
  ctorres$5->ctx_FileCons->rc ++;
  ctorres$5->head_FileCons = file;
  ctorres$5->head_FileCons->rc ++;
  ctorres$5->tail_FileCons = ctx->ref->files;
  ctorres$5->tail_FileCons->rc ++;
  $decr_FileList(actualCtx->files);
  actualCtx->files = ctorres$5;
  actualCtx->files->rc ++;
  struct Expr* ctorres$6 = malloc(sizeof (struct Expr));
  ctorres$6->rc = 0;
  ctorres$6->color = kBlack;
  ctorres$6->addedPCR = 0;
  ctorres$6->print = $print_Expr;
  ctorres$6->kind = Expr_tag;
  ctorres$6->file = file;
  ctorres$6->file->rc ++;
  struct Expr* expr = ctorres$6;
  expr->rc ++;
  struct ExprList* ctorres$7 = malloc(sizeof (struct ExprList));
  ctorres$7->rc = 0;
  ctorres$7->color = kBlack;
  ctorres$7->addedPCR = 0;
  ctorres$7->print = $print_ExprList;
  ctorres$7->kind = ExprCons_tag;
  ctorres$7->head_ExprCons = expr;
  ctorres$7->head_ExprCons->rc ++;
  ctorres$7->tail_ExprCons = file->exprs;
  ctorres$7->tail_ExprCons->rc ++;
  $decr_ExprList(file->exprs);
  file->exprs = ctorres$7;
  file->exprs->rc ++;
  actualCtx->files;
  struct Context* ctorres$8 = malloc(sizeof (struct Context));
  ctorres$8->rc = 0;
  ctorres$8->color = kBlack;
  ctorres$8->addedPCR = 0;
  ctorres$8->print = $print_Context;
  ctorres$8->kind = Context_tag;
  ctorres$8->name = "other context";
  struct FileList* ctorres$9 = malloc(sizeof (struct FileList));
  ctorres$9->rc = 0;
  ctorres$9->color = kBlack;
  ctorres$9->addedPCR = 0;
  ctorres$9->print = $print_FileList;
  ctorres$9->kind = FileNil_tag;
  ctorres$8->files = ctorres$9;
  ctorres$8->files->rc ++;
  $decr_Context(ctx->ref);
  ctx->ref = ctorres$8;
  ctx->ref->rc ++;
  file->exprs;
  ctx->ref;
  int ret$10 = 0;
  $decr_Expr(expr);
  $decr_Context(actualCtx);
  $decr_File(file);
  $decr_CtxRef(ctx);
  processAllPCRs();
  return ret$10;
}

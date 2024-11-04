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
enum CtxRef_kind { CtxRef_tag };
struct CtxRef {
  int rc;
  enum Color color;
  enum CtxRef_kind kind;
  struct Context* ref;
  union {
    struct {  };
  };
};
enum Context_kind { Context_tag };
struct Context {
  int rc;
  enum Color color;
  enum Context_kind kind;
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
  enum FileList_kind kind;
  union {
    struct {  };
    struct { struct Context* ctx_FileCons; struct File* head_FileCons; struct FileList* tail_FileCons; };
  };
};
enum File_kind { File_tag };
struct File {
  int rc;
  enum Color color;
  enum File_kind kind;
  struct ExprList* exprs;
  union {
    struct {  };
  };
};
enum ExprList_kind { ExprNil_tag, ExprCons_tag };
struct ExprList {
  int rc;
  enum Color color;
  enum ExprList_kind kind;
  union {
    struct {  };
    struct { struct Expr* head_ExprCons; struct ExprList* tail_ExprCons; };
  };
};
enum Expr_kind { Expr_tag };
struct Expr {
  int rc;
  enum Color color;
  enum Expr_kind kind;
  struct File* file;
  union {
    struct {  };
  };
};
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
int fn$addFile(struct Context* ctx, struct File* file);
int fn$addExpr(struct File* file, struct Expr* expr);
int main();
void $decr_CtxRef(struct CtxRef* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case CtxRef_tag:
      $decr_Context(this->ref);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_CtxRef,
      (void *) $scan_CtxRef,
      (void *) $collectWhite_CtxRef);
  }
}
void $decr_Context(struct Context* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Context_tag:
      $decr_FileList(this->files);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_Context,
      (void *) $scan_Context,
      (void *) $collectWhite_Context);
  }
}
void $decr_FileList(struct FileList* this) {
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
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_FileList,
      (void *) $scan_FileList,
      (void *) $collectWhite_FileList);
  }
}
void $decr_File(struct File* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case File_tag:
      $decr_ExprList(this->exprs);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_File,
      (void *) $scan_File,
      (void *) $collectWhite_File);
  }
}
void $decr_ExprList(struct ExprList* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case ExprNil_tag:
      break;
    case ExprCons_tag:
      $decr_Expr(this->head_ExprCons);
      $decr_ExprList(this->tail_ExprCons);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
      (void *) $markGray_ExprList,
      (void *) $scan_ExprList,
      (void *) $collectWhite_ExprList);
  }
}
void $decr_Expr(struct Expr* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Expr_tag:
      $decr_File(this->file);
      break;
    }
    removePCR(this);
    free(this);
  } else {
    addPCR(
      this,
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
    this->ref->rc --;
    $markGray_Context(this->ref);
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
    this->head_FileCons->rc --;
    $markGray_File(this->head_FileCons);
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
    $scan_Context(this->ref);
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
    $scan_File(this->head_FileCons);
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
      this->ref->rc ++;
      $scanBlack_Context(this->ref);
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
      this->head_FileCons->rc ++;
      $scanBlack_File(this->head_FileCons);
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
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
    struct FreeCell *curr = freeList;
    freeList = (void *) this;
    freeList->next = curr;
  }
}
int fn$addFile(struct Context* ctx, struct File* file) {
  ctx->rc ++;
  file->rc ++;
  struct FileList* ctorres$0 = malloc(sizeof (struct FileList));
  ctorres$0->rc = 0;
  ctorres$0->color = kBlack;
  ctorres$0->kind = FileCons_tag;
  ctorres$0->ctx_FileCons = ctx;
  ctorres$0->ctx_FileCons->rc ++;
  ctorres$0->head_FileCons = file;
  ctorres$0->head_FileCons->rc ++;
  ctorres$0->tail_FileCons = ctx->files;
  ctorres$0->tail_FileCons->rc ++;
  $decr_FileList(ctx->files);
  ctx->files = ctorres$0;
  ctx->files->rc ++;
  ctx->files;
  int ret$1 = 0;
  $decr_Context(ctx);
  $decr_File(file);
  return ret$1;
}
int fn$addExpr(struct File* file, struct Expr* expr) {
  file->rc ++;
  expr->rc ++;
  struct ExprList* ctorres$2 = malloc(sizeof (struct ExprList));
  ctorres$2->rc = 0;
  ctorres$2->color = kBlack;
  ctorres$2->kind = ExprCons_tag;
  ctorres$2->head_ExprCons = expr;
  ctorres$2->head_ExprCons->rc ++;
  ctorres$2->tail_ExprCons = file->exprs;
  ctorres$2->tail_ExprCons->rc ++;
  $decr_ExprList(file->exprs);
  file->exprs = ctorres$2;
  file->exprs->rc ++;
  file->exprs;
  int ret$3 = 0;
  $decr_File(file);
  $decr_Expr(expr);
  return ret$3;
}
int main() {
  struct CtxRef* ctorres$4 = malloc(sizeof (struct CtxRef));
  ctorres$4->rc = 0;
  ctorres$4->color = kBlack;
  ctorres$4->kind = CtxRef_tag;
  struct Context* ctorres$5 = malloc(sizeof (struct Context));
  ctorres$5->rc = 0;
  ctorres$5->color = kBlack;
  ctorres$5->kind = Context_tag;
  ctorres$5->name = "foo";
  struct FileList* ctorres$6 = malloc(sizeof (struct FileList));
  ctorres$6->rc = 0;
  ctorres$6->color = kBlack;
  ctorres$6->kind = FileNil_tag;
  ctorres$5->files = ctorres$6;
  ctorres$5->files->rc ++;
  ctorres$4->ref = ctorres$5;
  ctorres$4->ref->rc ++;
  struct CtxRef* ctx = ctorres$4;
  ctx->rc ++;
  struct File* ctorres$7 = malloc(sizeof (struct File));
  ctorres$7->rc = 0;
  ctorres$7->color = kBlack;
  ctorres$7->kind = File_tag;
  struct ExprList* ctorres$8 = malloc(sizeof (struct ExprList));
  ctorres$8->rc = 0;
  ctorres$8->color = kBlack;
  ctorres$8->kind = ExprNil_tag;
  ctorres$7->exprs = ctorres$8;
  ctorres$7->exprs->rc ++;
  struct File* file = ctorres$7;
  file->rc ++;
  struct Expr* ctorres$9 = malloc(sizeof (struct Expr));
  ctorres$9->rc = 0;
  ctorres$9->color = kBlack;
  ctorres$9->kind = Expr_tag;
  ctorres$9->file = file;
  ctorres$9->file->rc ++;
  struct Expr* expr = ctorres$9;
  expr->rc ++;
  struct Context* ctorres$10 = malloc(sizeof (struct Context));
  ctorres$10->rc = 0;
  ctorres$10->color = kBlack;
  ctorres$10->kind = Context_tag;
  ctorres$10->name = "other context";
  struct FileList* ctorres$11 = malloc(sizeof (struct FileList));
  ctorres$11->rc = 0;
  ctorres$11->color = kBlack;
  ctorres$11->kind = FileNil_tag;
  ctorres$10->files = ctorres$11;
  ctorres$10->files->rc ++;
  $decr_Context(ctx->ref);
  ctx->ref = ctorres$10;
  ctx->ref->rc ++;
  fn$addExpr(file, expr);
  ctx->ref;
  fn$addFile(ctx->ref, file);
  int ret$12 = 0;
  $decr_Expr(expr);
  $decr_File(file);
  $decr_CtxRef(ctx);
  processAllPCRs();
  return ret$12;
}

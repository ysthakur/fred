#include "runtime.h"

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
struct CtxRef* new$CtxRef(struct Context* ref);
struct Context* new$Context(struct FileList* files, char* name);
struct FileList* new$FileNil();
struct FileList* new$FileCons(struct Context* ctx, struct File* head, struct FileList* tail);
struct File* new$File(struct ExprList* exprs);
struct ExprList* new$ExprNil();
struct ExprList* new$ExprCons(struct Expr* head, struct ExprList* tail);
struct Expr* new$Expr(struct File* file);
int main();
void $free_CtxRef(struct CtxRef* this) {
  switch (this->kind) {
  case CtxRef_tag:
    $decr_Context(this->ref);
    break;
  }
  free(this);
}
void $free_Context(struct Context* this) {
  switch (this->kind) {
  case Context_tag:
    break;
  }
  free(this);
}
void $free_FileList(struct FileList* this) {
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
  switch (this->kind) {
  case File_tag:
    break;
  }
  free(this);
}
void $free_ExprList(struct ExprList* this) {
  switch (this->kind) {
  case ExprNil_tag:
    break;
  case ExprCons_tag:
    break;
  }
  free(this);
}
void $free_Expr(struct Expr* this) {
  switch (this->kind) {
  case Expr_tag:
    break;
  }
  free(this);
}
void $decr_CtxRef(struct CtxRef* this) {
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
  this->addedPCR = 0;
  switch (this->kind) {
  case CtxRef_tag:
    break;
  }
}
void $markGray_Context(struct Context* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
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
  this->addedPCR = 0;
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
  this->addedPCR = 0;
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
  this->addedPCR = 0;
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
  this->addedPCR = 0;
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
struct CtxRef* new$CtxRef(struct Context* ref) {
  struct CtxRef* $res = malloc(sizeof (struct CtxRef));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_CtxRef;
  $res->kind = CtxRef_tag;
  $res->ref = ref;
  $res->ref->rc ++;
  return $res;
}
struct Context* new$Context(struct FileList* files, char* name) {
  struct Context* $res = malloc(sizeof (struct Context));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Context;
  $res->kind = Context_tag;
  $res->name = name;
  $res->files = files;
  $res->files->rc ++;
  return $res;
}
struct FileList* new$FileNil() {
  struct FileList* $res = malloc(sizeof (struct FileList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_FileList;
  $res->kind = FileNil_tag;
  return $res;
}
struct FileList* new$FileCons(struct Context* ctx, struct File* head, struct FileList* tail) {
  struct FileList* $res = malloc(sizeof (struct FileList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_FileList;
  $res->kind = FileCons_tag;
  $res->ctx_FileCons = ctx;
  $res->ctx_FileCons->rc ++;
  $res->head_FileCons = head;
  $res->head_FileCons->rc ++;
  $res->tail_FileCons = tail;
  $res->tail_FileCons->rc ++;
  return $res;
}
struct File* new$File(struct ExprList* exprs) {
  struct File* $res = malloc(sizeof (struct File));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_File;
  $res->kind = File_tag;
  $res->exprs = exprs;
  $res->exprs->rc ++;
  return $res;
}
struct ExprList* new$ExprNil() {
  struct ExprList* $res = malloc(sizeof (struct ExprList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_ExprList;
  $res->kind = ExprNil_tag;
  return $res;
}
struct ExprList* new$ExprCons(struct Expr* head, struct ExprList* tail) {
  struct ExprList* $res = malloc(sizeof (struct ExprList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_ExprList;
  $res->kind = ExprCons_tag;
  $res->head_ExprCons = head;
  $res->head_ExprCons->rc ++;
  $res->tail_ExprCons = tail;
  $res->tail_ExprCons->rc ++;
  return $res;
}
struct Expr* new$Expr(struct File* file) {
  struct Expr* $res = malloc(sizeof (struct Expr));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Expr;
  $res->kind = Expr_tag;
  $res->file = file;
  $res->file->rc ++;
  return $res;
}
int main() {
  pcrBuckets = calloc(sizeof(void *), 3);
  numSccs = 3;
  struct CtxRef* ctx = new$CtxRef(new$Context(new$FileNil(), "foo"));
  ctx->rc ++;
  struct File* file = new$File(new$ExprNil());
  file->rc ++;
  struct Context* actualCtx = ctx->ref;
  actualCtx->rc ++;
  struct FileList* oldValue$0 = actualCtx->files;
  actualCtx->files = new$FileCons(ctx->ref, file, ctx->ref->files);
  actualCtx->files->rc ++;
  $decr_FileList(oldValue$0);
  $decr_Context(actualCtx);
  struct Expr* expr = new$Expr(file);
  expr->rc ++;
  struct ExprList* oldValue$1 = file->exprs;
  file->exprs = new$ExprCons(expr, file->exprs);
  file->exprs->rc ++;
  $decr_ExprList(oldValue$1);
  drop((void *) file->exprs, (void *) $decr_ExprList);
  $decr_Expr(expr);
  $decr_File(file);
  struct Context* oldValue$2 = ctx->ref;
  ctx->ref = new$Context(new$FileNil(), "other context");
  ctx->ref->rc ++;
  $decr_Context(oldValue$2);
  drop((void *) ctx->ref, (void *) $decr_Context);
  $decr_CtxRef(ctx);
  int ret$3 = 0;
  processAllPCRs();
  free(pcrBuckets);
  return ret$3;
}

enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;

  union {
    struct { int x_Bar; int y_Bar; };
    struct { char* a_Baz; int b_Baz; };
  };
};
int fn$foo(struct Foo* foo) {
  
  struct Foo* matchobjres$0 = foo;
  int matchresres$1;
  switch (matchobjres$0->kind) {
  case Bar_tag:
    int x = matchobjres$0->x_Bar;
    
    int yyy = matchobjres$0->y_Bar;
  
    matchresres$1 = x + yyy;
  
  
    break;
  case Baz_tag:
    char* astr = matchobjres$0->a_Baz;
    
    int b = matchobjres$0->b_Baz;
    
    matchresres$1 = b;
    
  
    break;
  }
  int fnres$2 = matchresres$1;
  
  return fnres$2;
}
int main() {
  struct Foo* ctorres$3 = malloc(sizeof (struct Foo));
  ctorres$3->kind = Bar_tag;
  
  ctorres$3->x_Bar = 1;
  
  
  
  ctorres$3->y_Bar = 2;
  
  
  struct Foo* foo = ctorres$3;
  foo->rc ++;
  int fnres$4 = fn$foo(foo);
  
  
  if (--foo->rc == 0) {
    free(foo);
  }
  return fnres$4;
}
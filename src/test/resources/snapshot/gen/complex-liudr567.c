enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  int rc;
  enum Foo_kind kind;

  union {
    struct { int x_Bar; int y_Bar; };
    struct { char* a_Baz; int b_Baz; };
  };
};
int fn$foo (struct Foo* foo) {
  
  struct Foo* var$43069336 = foo;
  int var$5ef20a99;
  switch (var$43069336->kind) {
  case Bar_tag:
    int x = var$43069336->x_Bar;
    
    int yyy = var$43069336->y_Bar;
  
    var$5ef20a99 = x + yyy;
  
  
    break;
  case Baz_tag:
    char* astr = var$43069336->a_Baz;
    
    int b = var$43069336->b_Baz;
    
    var$5ef20a99 = b;
    
  
    break;
  }
  int var$915130db = var$5ef20a99;
  
  return var$915130db;
}
int main () {
  struct Foo* var$9a014472 = malloc(sizeof (struct Foo));
  var$9a014472->kind = Bar_tag;
  
  var$9a014472->x_Bar = 1;
  
  
  
  var$9a014472->y_Bar = 2;
  
  
  struct Foo* foo = var$9a014472;
  foo->rc ++;
  int var$3c4e873c = fn$foo(foo);
  
  
  if (--foo->rc == 0) {
    free(foo);
  }
  return var$3c4e873c;
}
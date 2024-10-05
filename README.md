# fred

CMSC499 project

## Compiling to C

A type like
```
data Foo
  = Bar {
      mut foo: Foo,
      fred: Fred,
      common: str,
      notcommon: str
    }
  | Baz {
      blech: str,
      mut gah: int,
      common: str,
      notcommon: int
    }
```
would be compiled to the following:
```c
enum Foo_kind { Bar_tag, Baz_tag };
struct Foo {
  enum Foo_kind kind;
  char* common;
  union {
    struct { struct Foo* foo_Bar; struct Fred* fred_Bar; char* notcommon_Bar; };
    struct { char* blech_Baz; int gah_Baz; int notcommon_Baz; };
  };
};
```

Every field has the name of its variant added to it, so that multiple variants can have fields with the same name (e.g. `notcommon`). But fields that have the same type in all variants are put outside the union, e.g., `common`, which has a type of `str` in all variants.

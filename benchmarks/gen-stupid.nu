#!/usr/bin/env nu

# Generate a benchmark where lazy mark scan outperforms my algorithm
# stupid because this has nothing to do with the real world

let numTypes = 200

for type in 0..<$numTypes {
  let next = if $type == 0 { "" } else { $", f: T($type - 1)" }
  print $"data T($type) = T($type) { mut cyclic: OptT($type)($next) }"
  print $"data OptT($type) = SomeT($type) { value: T($type) } | NoneT($type) {}"
}

print $"fn create\(): T($numTypes - 1) ="
let v0 = $"T0 { cyclic: NoneT0 {} }"
print (1..<$numTypes | reduce -f $v0 {|t, acc| $"T($t) { cyclic: NoneT($t) {}, f: ($acc) }" })

print $"fn triggerDecrs\(v: T($numTypes - 1)): int ="
for nesting in 0..<$numTypes {
  let ref = 0..<$nesting | each { ".f" } | str join
  print $"\(let temp($nesting) = v($ref) in 0);"
}
print "0"

print '
fn runOnce(): int =
  let v = create() in
  triggerDecrs(v)

fn run(iters: int): int =
  if iters == 0 then 0
  else
    runOnce();
    c("processAllPCRs();");
    run(iters - 1)

fn main(): int =
  printf("Starting stupid benchmark\n");
  c("float clockStart = (float) clock()/CLOCKS_PER_SEC;");
  c("u_int64_t tscStart = rdtscp();");
  run(50000);
  c("printf(
      \"Time stamp counter diff: %ld, clock diff: %lf\",
      rdtscp() - tscStart,
      ((float) clock()/CLOCKS_PER_SEC) - clockStart);")'

const jar = "fred.jar"
const outExe = "bench.bin"

export def run [ file: string, lazyMarkScanOnly: bool, allSccsBad: bool ] {
  let benchName = $file | path basename
  let opts = [
    ...(if $lazyMarkScanOnly { [--lazy-mark-scan-only] } else { [] }),
    ...(if $allSccsBad { [--all-sccs-bad] } else { [] })
  ]
  java -jar $jar $file -o $outExe ...$opts
  print $"Compiled ($file) \(lazy-mark-scan-only: ($lazyMarkScanOnly), all-sccs-bad: ($allSccsBad))"
  let out = ^$"./($outExe)"
  print $out
  let res = (
      $out
      | lines
      | parse "Time stamp counter diff: {tsc}, clock diff: {clock}"
      | get 0)
  {
    Name: $benchName,
    "Timestamp counter": $res.tsc,
    Clock: $res.clock,
    "Lazy mark scan only": $lazyMarkScanOnly,
    "All SCCs bad": $allSccsBad,
  }
}

export def run-all [ files: glob = benchmarks/*.fred ] {
  let results = glob $files | each { |file|
      [
        (run $file false false),
        (run $file false true),
        (run $file true false),
        (run $file true true)
      ]
    }
  $results | flatten
}

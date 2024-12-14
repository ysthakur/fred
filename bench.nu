const jar = "fred.jar"
const outExe = "bench.bin"

export def run [ file: string, lazyMarkScanOnly: bool ] {
  let benchName = $file | path basename
  if $lazyMarkScanOnly {
    java -jar $jar $file -o $outExe --lazy-mark-scan-only
  } else {
    java -jar $jar $file -o $outExe
  }
  print $"Compiled ($file) \(lazy-mark-scan-only: ($lazyMarkScanOnly))"
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
    "Lazy mark scan only": $lazyMarkScanOnly
  }
}

export def run-all [ ] {
  let files = (ls benchmarks/*.fred).name
  let results = $files | each { |file| [(run $file false), (run $file true)] }
  $results | flatten
}

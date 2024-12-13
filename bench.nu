const jar = "fred.jar"
const outExe = "bench.bin"

export def run [ file: string, lazyMarkScan: bool ] {
  let benchName = $file | path basename
  if $lazyMarkScan {
    java -jar $jar $file -o $outExe --lazy-mark-scan-only
  } else {
    java -jar $jar $file -o $outExe
  }
  let res = (
    ^$"./($outExe)"
      | lines
      | last
      | parse "Time stamp counter diff: {tsc}, clock diff: {clock}"
      | get 0)
  {
    name: $benchName,
    tsc: $res.tsc,
    clock: $res.clock,
    lazyMarkScan: $lazyMarkScan
  }
}

export def run-all [ ] {
  let files = (ls benchmarks).name
  let myAlgo = $files | each { |file| run $file false }
  let lazyOnly = $files | each { |file| run $file true }
  $myAlgo ++ $lazyOnly
}

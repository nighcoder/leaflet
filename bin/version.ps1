Select-String -Pattern '\(defproject clojupyter-plugin/widgets "(.+)"' -Path project.clj | % { $_.Matches.Groups[1].Value }

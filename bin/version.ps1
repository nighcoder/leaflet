Select-String -Pattern '\(defproject clojupyter-plugin/leaflet "(.+)"' -Path project.clj | % { $_.Matches.Groups[1].Value }

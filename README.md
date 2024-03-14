# 🫧 Bubble C🫧nfig

An [aero](https://github.com/juxt/aero) powered config with environments aimed at Babashka tasks.

<p align="center">
<a href="https://polar.sh/eval"><picture><source media="(prefers-color-scheme: dark)" srcset="https://polar.sh/embed/subscribe.svg?org=eval&label=Subscribe&darkmode"><img alt="Subscribe on Polar" src="https://polar.sh/embed/subscribe.svg?org=eval&label=Subscribe"></picture></a>
</p>

## Rationale

I wanted to make my Babashka tasks simpler. Instead of doing `bb some-task --flag1 val1 --flag2 val2`, this library allows you to do `bb some-task -e dev` and the right flags for the environment will be read from a config-file and passed to the task.

## Usage

### Babashka tasks

This library is ideal for Babashka tasks as it allows you to call tasks with predefined flags from a config-file.

Say we have a CLI, clojore.core/prn, and create a task from it:

```clojure
;; bb.edn
:tasks {
  prn (exec 'clojure.core/prn)
}
```

Works as expected:
```shell
$ bb prn -a 1 -b 2
{:a 1, :b 2}
```

Now when you have a lot of flags that also differ between, say, local development, CI and production, then it's convenient to have those environment-specific 'sets' of flags in one config.  
This is what Bubble Config allows you to do:

```clojure
:deps  {io.github.eval/bubble-config {:git/sha "fd8a252aa49c83abb3aab1e523f749101e8c1a86"}}
:tasks {:init
         (do
           (defn config []
             (exec 'bubble-config.core/config)))

  prn (exec 'clojure.core/prn {:exec-args (config)})
}
```

Install a sample config:
```bash
$ bb -x bubble-config.core/sample > config.edn
```

Now we get:

```shell
# dev environment
$ bb prn
{:a 0, :b 1}

# test environment via flag
$ bb prn -e test
{:a 0, :b 2, :e "test"}

# test environment via env-var
$ env BBL_ENV=test bb prn
{:a 0, :b 2}

# config overrides
$ bb prn -e test -a 2
{:a 2, :b 2, :e "test"}
```

So instead of having long commands that differ in every environment, `bb some-task` suffices everywhere. Switching between envs is easy, as is inspecting what the flags per environment look like.


### CLI

``` bash
$ export BBL_DEPS='{:deps {io.github.eval/bubble-config {:git/sha "fd8a252aa49c83abb3aab1e523f749101e8c1a86"}}}'

# Babashka/Clojure commands side by side
# a sample config
$ bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/sample | tee config.edn
$ clojure -Sdeps "${BBL_DEPS}" -X bubble-config.core/sample | tee config.edn

# print config
# the default environment is the first of the detected environments in config.edn
# meta-data shows, among other things, what envs were detected (left out in following examples).
$ bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/print
$ clojure -Sdeps "${BBL_DEPS}" -X bubble-config.core/print
^{:bubble-config/available-envs (:dev :test :prod),
  :bubble-config/config-file "config.edn",
  :bubble-config/current-env :dev}
{:a 0, :b 1}

# different environment via env-var
$ env BBL_ENV=prod bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/print
$ env BBL_ENV=prod clojure -Sdeps "${BBL_DEPS}" -X bubble-config.core/print
{:a 0, :b 3}

# different environment via the env-flag (overrides env-var)
$ bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/print -e test
$ clojure -Sdeps "${BBL_DEPS}" -X bubble-config.core/print :env test
{:a 0, :b 2}

# full help
$ bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/print -h
$ clojure -Sdeps "${BBL_DEPS}" -X bubble-config.core/print :help true

# unknown env raises assert-exception
$ bb -Sdeps "${BBL_DEPS}" -x bubble-config.core/print -e foo
```


## LICENSE

Copyright (c) 2024 Gert Goet, ThinkCreate.
Distributed under the MIT license. See [LICENSE](LICENSE).

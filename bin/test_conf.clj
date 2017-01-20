(require '[mount.core :as mount])

(mount/start)

(change-defaults :emitter 'midje.emission.plugins.junit
                 :print-level :print-facts
                 :colorize false)

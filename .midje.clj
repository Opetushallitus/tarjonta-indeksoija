(change-defaults :emitter 'midje.emission.plugins.junit
                 :print-level :print-facts
                 :colorize false)

(require '[mount.core :as mount])

(mount/start)
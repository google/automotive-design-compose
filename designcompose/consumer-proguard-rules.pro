# Must keep all generated messages
-shrinkunusedprotofields

# I can't find the exact reason why, but ignoring this warning seems to be a standard thing to do.
# Including in the grpc examples
-dontwarn javax.naming.**
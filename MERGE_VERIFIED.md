# Eclient + ProtoHax merge (verified source tree)

This source tree was rebuilt from the uploaded Eclient base and changed in-place.

Gameplay module directories now contain:
- combat/EClientKillAura.kt
- combat/EClientCrystalAura.kt
- combat/EClientInfiniteAura.kt
- combat/EClientVelocity.kt
- combat/EClientCriticalHit.kt
- movement/EClientNoFall.kt
- movement/EClientSprint.kt
- movement/EClientSpeed.kt
- movement/EClientFly.kt
- movement/EClientBlink.kt
- movement/EClientScaffold.kt
- movement/MotionFly.kt (the only retained legacy Rubidium gameplay module)

The old combat/movement gameplay files were removed from those directories.
Visual/overlay support modules remain because the Android UI directly imports them; they are not registered in ModuleManager.

The ClickGUI source is now EClientClickGui.kt and the saved UI preference migrates the old PROTO value to ECLIENT.

ProtoHax relay integration remains through ProtoHaxFrameIdCodec and the existing Rubidium relay/session implementation.

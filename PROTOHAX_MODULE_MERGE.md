# ProtoHax-derived module merge

The registered gameplay module set is now replaced by ProtoHax-derived modules adapted to the Eclient/Rubidium packet/event API.

Registered modules:
- EClientKillAura — adapted from ProtoHax ModuleKillAura targeting/rotation concepts.
- EClientCrystalAura — adapted from the ProtoHax CrystalAura role and Eclient's Bedrock placement/attack primitives.
- EClientInfiniteAura — adapted from ProtoHax InfiniteAura semantics.
- EClientVelocity — ProtoHax Velocity packet behavior.
- EClientCriticalHit — ProtoHax CriticalHit modes, including the Easecation height sequence.
- EClientNoFall — ProtoHax NoFall packet protection.
- EClientSprint — ProtoHax Sprint packet behavior.
- EClientSpeed — ProtoHax Speed-style movement modes adapted to the Eclient packet API.
- EClientFly — ProtoHax Fly-style movement adapted to the Eclient packet API.
- EClientBlink — ProtoHax Blink packet buffering adapted to the Eclient packet bus.
- EClientScaffold — ProtoHax Scaffold-style block placement adapted to Eclient.
- MotionFly — the only legacy Rubidium gameplay module intentionally retained.

The old combat/movement gameplay module source files were removed from the normal module directories rather than merely being hidden from ModuleManager. Visual/overlay support modules that the Android UI still directly depends on remain separate and are not registered as gameplay modules.

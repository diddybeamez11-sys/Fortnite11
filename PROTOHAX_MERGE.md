# ProtoHax integration

This build keeps the Eclient/Rubidium Android relay/session architecture and integrates ProtoHax relay behavior where the two systems overlap.

Integrated:
- ProtoHax-compatible frame-ID codec behavior through `ProtoHaxFrameIdCodec`.
- The codec is installed on both client-facing and upstream relay channels.
- Reliability uses `RELIABLE_ORDERED` semantics.
- The registered gameplay module set is now ProtoHax-derived and adapted to Eclient's packet/event API.
- The ClickGUI is now `EClientClickGui` and is wired to the new module registry.

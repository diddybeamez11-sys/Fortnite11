# E-Client standalone native runtime — Minecraft 1.21.80

E-Client now targets Minecraft Bedrock **1.21.80 / protocol 800 / ARM64-v8a** without a Levi/Preloader dependency.

## Native target fingerprint

The supplied target library is:

- `lib/arm64-v8a/libminecraftpe.so`
- SHA-256: `84cc649545f95420212038dd483896d5d53507e25a309e68bd038b0256a976ae`
- GNU Build ID: `665d33595ddec90fe7347a45cd4da65e6f0871aa`
- Size: `247512600` bytes
- ELF: 64-bit AArch64 shared object

## Runtime architecture

The E-Client APK bundles `libeclient_runtime.so` and controls it through JNI. The native runtime validates the 1.21.80 target profile and exposes diagnostics over loopback on `127.0.0.1:38170`.

This is intentionally **not** described as Minecraft-process injection. Android's normal application process cannot simply load the installed Minecraft engine into the E-Client process and thereby become Minecraft. A future in-process attachment/launcher mechanism must be implemented and validated separately before native game hooks can be claimed.

## Version-specific native work

Do not copy offsets, vtables, or function addresses from 1.21.70. The 1.21.80 adapter must be derived from the exact ARM64 library above. The current runtime only validates the binary identity and keeps gameplay hooks disabled until a real signature/structure mapping is available.

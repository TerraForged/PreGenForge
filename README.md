# PreGenForge
Forge chunk pregenerator

## Commands

#### Start

Starts a standard pregenerator which generates an area of chunks.

Note - The radius is in **chunks**. The total length of one side of the generated area will be roughly **`2 x <radius>`**

- `/pregen start <radius>` - start a pregenerator centered on the world spawn
- `/pregen start <radius> <dimension>` - start a pregenerator centered on the world spawn in the given dimension
- `/pregen start <radius> <x> <z>` - start a pregenerator centered on the x,z coordinates
- `/pregen start <radius> <x> <z> <dimension>` - start a pregenerator centered on the x,z coordinates in the given dimension

#### Expand

Starts a pregenerator but skips an inner-radius of chunks - this is useful if you have already generated an area of
chunks and don't want to waste resources loading those chunks from disk into memory.

Note - Radii are in **chunks**.

- `/pregen expand <inner_radius> <outer_radius>` - start an 'expand' pregenerator centered on the world spawn
- `/pregen expand <inner_radius> <outer_radius> <dimension>` - start an 'expand' pregenerator centered on the world
 spawn in the given dimension
- `/pregen expand <inner_radius> <outer_radius> <x> <z>` - start an 'expand' pregenerator centered on the x,z coordinates
- `/pregen expand <inner_radius> <outer_radius> <x> <z> <dimension>` - start an 'expand' pregenerator centered on the
 x,z coordinates in the given dimension
 
#### Pause

Temporarily pause/unpause a pregenerator.

Note - paused pregenerators will start automatically the next time the server starts

- `/pregen pause` - pause the pregenerator until unpaused
- `/pregen resume` - unpause a paused pregenerator

#### Cancel

`/pregen cancel` - stop and remove a pregenerator

#### Notify

`/pregen` notify <true | false> - toggle pregeneration status & progress messages

#### Time

It might be desirable to reset the world & game time after pregenerating, as it can take a while depending on
 how large an area is being generated.

`/pregen time <ticks> <dimension>` - set the world and game time (in ticks) in the given dimension
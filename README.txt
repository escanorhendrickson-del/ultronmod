ULTRON MOD v1.2 - Minecraft 1.20.1 (Forge 47.3.0) - SOURCE PROJECT

GET THE .JAR (no install needed): GitHub Actions
1. Upload ALL files in this folder to your GitHub repo (overwrite existing).
   The .github/workflows/build.yml must exist (create it by hand if the upload skips it).
2. Actions tab -> wait for the green check -> open the run -> download "ultronmod-jar".
3. Unzip it, put the .jar in your Forge 1.20.1 mods folder.
   (Delete older ultronmod jars first!)

ULTRON BOSS (Spawn Eggs tab)
- Custom 3D model, 2x-detail skin + glow layer (eyes, chest, joints, tubing)
- Repulsor beam from the hand
- Unibeam: charges up (particles converge on chest) then fires a 32-block beam
- Ground Slam: arms up, shockwave ring, knockback + damage
- Overdrive at 50% HP: lightning, faster, harder hitting, smoke trail, purple boss bar
- Dramatic multi-explosion death

ULTRON GAUNTLET (Combat tab, or 5 iron + 1 Ultron Core)
- Right-click: repulsor blast
- Sneak + right-click: toggle flight / no fall damage
- In hand: Resistance + Regeneration

EDITING THE LOOK
- Skin: assets/ultronmod/textures/entity/ultron.png (and ultron_glow.png for what lights up)
- tools/generate_assets.py regenerates model + skin (needs python + Pillow)
- tools/preview_front.png shows a flat front preview

# SkriptPackager
Turn Skripts in to Jars

SkriptPackagerGUI does this:
- Gets user input
- Clones this repository
- Changes com.gmail.xxandrew28xx to skript<1-100>.<skript name> where <> indicates that value can be different things
- Change package declarations to      ^
- Edits config.yml & plugin.yml
- mvn install
- Gives jar to user

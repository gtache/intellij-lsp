# Contributing
## Targeting the right branch
Assuming the current release is A.B.C, there should be two branches: A.B.(C+1) and A.(B+1).0.     
- If you are fixing a bug or implementing a very small feature, you should target the A.B.(C+1) branch.
- If you are implementing a sizeable new feature, you should target A.(B+1).0
- If you are modifying something that affects both (i.e. not a bugfix or a feature), you may target master

## Importing and working on the project
Clone the repo and checkout the correct branch. Afterwards, the easiest way is to create a new IntelliJ Platform Plugin project with Scala support and then :
- Copy the src and resources folder into your project
- Specify the jdk if needed: Create a new IntelliJ Platform Plugin SDK and target your IntelliJ folder (it should be automatically selected)
- Adds the dependencies through Maven or download the libs yourself. They are noted in the build.gradle file. For now, these are : 
  - coursier
  - coursier-cache
  - lsp4j
  - flexmark
- You should be able to use the "Run" button to start an IntelliJ sandbox with the current development plugin automatically installed in it.
- You can start coding!

- Or use the build.gradle file, which should work (but I'm not personally using it).

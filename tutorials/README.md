# Tutorials

This area is comprised of tutorials that focus on various aspects of the AoU workbench code base, such as: code organization, build processes, tools & technologies, back end API, database and front end architecure.

This is intended to help folks onboard faster and get a solid understanding of the AoU code base. It can also be used by AoU veterans who are not comfortable with certain aspects of the code or want to learn more about a certain area of the code.
## How to Use And Contribute To Tutorials
These tutorials are meant to be living documents. If there is an issue with any tutorial, or something needs updating feel free to fix it and put up a PR.

If you have learned something new and think it will help the next person or see something that needs improvement file a PR to ensure other members - current and future - can benefit from what you have learned and your unique perspective.

We should look to have a consistent view of each tutorial. As a dev ramps up on a topic they will not have to expend mental energy dealing with a new tutorial style - similar to linting our code and keeping the style consistent.

When creating a new tutorial please add the following sections to the document:

1. **Overview**
   1. This section will inform the user of the tutorial about what aspects of the code the tutorial will cover and what they should get out of it
2. **Resources**
   1. A listing of resources/tools that are used in the tutorial
3. **Dependencies** (Optional)
   1. If a tutorial needs some setup involved you can add a dependencies section outlining the prereqs or additional install steps
4. **Tasks**
   1. This should be a section of step by step instructions the user can go through for hands on experience working with the code. The instructions should try to include some questions for the user to think about as they go through the tasks.
   2. The tasks should be clear and provide detailed steps when it comes to our specific implementation details, and links to external resources for things like tools and external libraries
   3. Please give each task a label. This will allow someone using the tutorial to clearly ask a team member for help on a given task if needed.
5. **Caveats / Troubleshooting** (Optional)
   1. There may be some unknowns or flaws in the process that have some workarounds associated with the tutorial. This is a place where we can call those out.
   2. We could also ask the person taking the tutorial to try fixing the issue
      1. As these types of tech debt issues crop up, we can identify them in stories and either have them addressed as learning/onboarding tasks or take care them if it looks like it will be a while before the team will be bringing on a new developer.
6. **Comprehension Questions**
   1. This section should be comprised of open ended questions that the user should be able to answer at the end of the tutorial.
   2. These can be difficult and may require the user to do some additional research to better understand the technology they just utilized.
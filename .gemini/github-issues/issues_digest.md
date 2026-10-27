# Open GitHub Issues Digest: `google/automotive-design-compose`

**Total Open Issues:** 95

---

## Table of Contents

- [#75: Create method for fetching the serialized Figma files for an app via Gradle task](#issue-75) (0 comments)
- [#93: Add method to input Figma Design Token via Design Switcher](#issue-93) (1 comments)
- [#95: Add emulator tests to CI](#issue-95) (4 comments)
- [#97: Create method for setting the Figma Token once per device](#issue-97) (1 comments)
- [#101: Automatically stage Release Candidates to gMaven](#issue-101) (1 comments)
- [#103: Allow for removal of Live Update code from production app](#issue-103) (0 comments)
- [#112: "Favorite docs" - Add method for setting alternate Figma Files to load in the Design Switcher](#issue-112) (1 comments)
- [#120: Re-make doc page for Custom Fonts](#issue-120) (0 comments)
- [#121: Re-make doc page on Figma Interactions](#issue-121) (0 comments)
- [#131: Add documentation covering Dials & Gauges for developers](#issue-131) (2 comments)
- [#132: Reorganize docs into a new set of categories](#issue-132) (2 comments)
- [#136: Impossible to fetch node under intersection/union](#issue-136) (11 comments)
- [#177: Implement lightweight theming using Variables](#issue-177) (0 comments)
- [#225: Live update stops working with frames containing large number of layers](#issue-225) (10 comments)
- [#250: Replace all references to Figma "documents" with "file"in the source code / API](#issue-250) (0 comments)
- [#268: Add Fetcher support for figma token file](#issue-268) (0 comments)
- [#269: Manifest support for Fetcher](#issue-269) (0 comments)
- [#270: Convert existing Kotlin JSON generation in Common and DesignCompose to use kotlinx.serialization](#issue-270) (0 comments)
- [#271: Update Kotlin and Rust to new Manifest schema](#issue-271) (1 comments)
- [#272: Incremental update support for Fetcher](#issue-272) (0 comments)
- [#273: Create Gradle figmaFileFetch task, including bundling the Fetcher tool with the plugin.](#issue-273) (0 comments)
- [#274: Connect the figmaFileFetch task to the output of the Codegen task, add DSL, update documentation.](#issue-274) (0 comments)
- [#275: Create figmaFileCommit and figmaFileFetch andCommit tasks, create developer script to update all serialized files in the repository.](#issue-275) (0 comments)
- [#278: Document using DesignCompose to create a traditional Android View](#issue-278) (0 comments)
- [#282: Consider making an allowlist for recursive doc fetches](#issue-282) (0 comments)
- [#290: Cleanup rectangle / bounding box setup](#issue-290) (0 comments)
- [#395: Layout is broken on autolayout frames that are rotated](#issue-395) (0 comments)
- [#398: Text alignment with multiple lines is incorrect](#issue-398) (1 comments)
- [#469: Separate the JNI crate into Live Update and Layout](#issue-469) (1 comments)
- [#551: DesignCompose does not download images larger than a certain size](#issue-551) (1 comments)
- [#561: Performance optimization for Rust layout implementation](#issue-561) (0 comments)
- [#573: Make Brush function customization accept an `ImageContext`](#issue-573) (0 comments)
- [#604: Add support for baseline alignment](#issue-604) (0 comments)
- [#606: Investigate transition to Kotlin 2.0 and KSP 2.0](#issue-606) (0 comments)
- [#628: Galaxy design crashes emulators on APIs greater than 33](#issue-628) (1 comments)
- [#635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor](#issue-635) (0 comments)
- [#649: Provide a means for users to access the Validation docs](#issue-649) (3 comments)
- [#661: Improve documentation on finding and using the generated JSON file for Lists & Grids](#issue-661) (0 comments)
- [#673: Support scale constraints with vectors](#issue-673) (0 comments)
- [#684: Configure Firebase Test Lab, add performance testing](#issue-684) (0 comments)
- [#688: Nightly Experimental builds failing](#issue-688) (0 comments)
- [#690: Improve handling of AAOS Unbundled Libraries in GitHub Actions](#issue-690) (0 comments)
- [#708: Modify Tutorial instructions to run "setFigmaTokenDebug" from Android Studio instead of the command line.](#issue-708) (0 comments)
- [#709: Enhance "SetFigmaToken" to target the actively running Android Device](#issue-709) (0 comments)
- [#710: Feature idea: Automatically add Compose test tags to all DesignCompose customizations](#issue-710) (0 comments)
- [#711: Test library - Create examples of various tests](#issue-711) (0 comments)
- [#712: Documentation - Document compile-time optimizations](#issue-712) (0 comments)
- [#713: Tutorial - consider alternative instructions for loading your Figma Doc ID](#issue-713) (0 comments)
- [#739: Design Component must be within the main document Figma interface](#issue-739) (2 comments)
- [#840: Variant prototype not working when the elements come from a library](#issue-840) (12 comments)
- [#883: Consolidate JNI Library artifacts into a single Gradle module](#issue-883) (0 comments)
- [#890: Use version catalog for exoplayer test dependency](#issue-890) (1 comments)
- [#905: Masks inside of variants do not render correctly](#issue-905) (6 comments)
- [#954: Enable DesignModule usage across Gradle projects](#issue-954) (0 comments)
- [#1031: Vectors in Serialized Documents Don't Provide all Colors](#issue-1031) (2 comments)
- [#1117: ImageContext does not provide useful data when node constraints set to scale](#issue-1117) (0 comments)
- [#1118: Provide additional test cases for external renderer comparison.](#issue-1118) (0 comments)
- [#1138: Custom Design Modules must be in same directory as Generated Composable](#issue-1138) (1 comments)
- [#1167: How to scale the Figma objects in the emulator based on the screen DPI](#issue-1167) (1 comments)
- [#1169: Support duplicate variable names across multiple documents](#issue-1169) (0 comments)
- [#1170: Scaling two separates instances of a component will always take the original size](#issue-1170) (1 comments)
- [#1187: Fill with image not showing on the emulator](#issue-1187) (6 comments)
- [#1295: Localization plugin UI polish](#issue-1295) (1 comments)
- [#1299: Control Figma UI prototype state in code](#issue-1299) (1 comments)
- [#1300: Drag-Drop implementation](#issue-1300) (2 comments)
- [#1331: DesignVariant not working with figma component instances](#issue-1331) (11 comments)
- [#1368: More flexibility possible?](#issue-1368) (12 comments)
- [#1373: Create a better separation between toolkit_schema and figma_schema](#issue-1373) (0 comments)
- [#1419: This would be a candidate for writing an `Into` implementation in figma_schema](#issue-1419) (0 comments)
- [#1446: Animate between design token values](#issue-1446) (0 comments)
- [#1450: Begin tightly vetting Rust dependencies](#issue-1450) (0 comments)
- [#1544: Improve error handling: propagate an error from Rust and show it in DesignSwitcher](#issue-1544) (0 comments)
- [#1555: More support for text stroke](#issue-1555) (0 comments)
- [#1568: Determine the plan for managing Rust dependencies](#issue-1568) (0 comments)
- [#1690: Figma Collections, Modes, Variables not available for query from design compose.](#issue-1690) (0 comments)
- [#1721: Dropdown list,  Slider bar support](#issue-1721) (1 comments)
- [#1792: Squoosh enabled cannot find variable for components imported from library](#issue-1792) (1 comments)
- [#1798: Improvements for DesignSwitcher](#issue-1798) (1 comments)
- [#1814: Better support for RRO](#issue-1814) (2 comments)
- [#1994: Make component library with DC](#issue-1994) (1 comments)
- [#2113: Component overrides next](#issue-2113) (0 comments)
- [#2138: RB HVAC investigation](#issue-2138) (0 comments)
- [#2145: RTL support](#issue-2145) (0 comments)
- [#2181: Customizations are not propagated to replacement component children](#issue-2181) (0 comments)
- [#2183: Investigate improvements for testing for nodes](#issue-2183) (0 comments)
- [#2188: Add support for insets in the Scalable UI plugin](#issue-2188) (0 comments)
- [#2189: Add support for rounded corners in Scalable UI](#issue-2189) (0 comments)
- [#2190: Allow progress bar and indicator preview simultaneously](#issue-2190) (0 comments)
- [#2208: font family variables support](#issue-2208) (0 comments)
- [#2245: Scope Figma Draw features](#issue-2245) (1 comments)
- [#2261: Same Figma node names do not execute smart animations.](#issue-2261) (0 comments)
- [#2330: Layers with opacity start flickering when placed on another layer with opacity](#issue-2330) (0 comments)
- [#2332: Component Instances are not render correctly until detached](#issue-2332) (1 comments)
- [#2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens](#issue-2401) (0 comments)
- [#2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext](#issue-2456) (1 comments)

---

## <a id="issue-75"></a>Issue #75: Create method for fetching the serialized Figma files for an app via Gradle task

- **URL:** [https://github.com/google/automotive-design-compose/issues/75](https://github.com/google/automotive-design-compose/issues/75)
- **Author:** @timothyfroehlich
- **Created:** 2023-05-19T21:17:17Z
- **Updated:** 2026-03-17T16:38:48Z
- **Labels:** `Type: Customer Issue`, `engProd`, `:green_circle:  P2`
- **Assignees:** *None*

### Description

This would be a way to fetch all of the files that an app will use.

I think the "default" task would fetch them to a location in the build folder, and a seperate task would copy those into the src/main/assets/figma folder so that they could be committed.

This task would make resolving #73 much easier.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-93"></a>Issue #93: Add method to input Figma Design Token via Design Switcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/93](https://github.com/google/automotive-design-compose/issues/93)
- **Author:** @timothyfroehlich
- **Created:** 2023-05-31T21:27:20Z
- **Updated:** 2026-03-17T16:38:39Z
- **Labels:** `Type: New Feature`, `needs design`, `qol`, `:blue_square:  P3`, `top-qol`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (1)

#### Comment #1 by @timothyfroehlich on 2024-06-20T20:24:29Z

Need a better description.


---

## <a id="issue-95"></a>Issue #95: Add emulator tests to CI

- **URL:** [https://github.com/google/automotive-design-compose/issues/95](https://github.com/google/automotive-design-compose/issues/95)
- **Author:** @timothyfroehlich
- **Created:** 2023-05-31T21:28:47Z
- **Updated:** 2026-03-17T16:38:35Z
- **Labels:** `needs design`, `engProd`, `:exclamation: P1`, `Type: Task`
- **Assignees:** *None*

### Description

We have a set of instrumented Android tests that can only run on an emulator or device. Currently we are not running these tests automatically, either in presubmit or postsubmit. 

There are a few ways this could be done (Firebase Test Lab, running an emulator on a GitHub runner).


### Comments (4)

#### Comment #1 by @timothyfroehlich on 2023-05-31T21:29:32Z

dupe of #76


#### Comment #2 by @timothyfroehlich on 2023-06-23T16:25:25Z

Had to revert the emulator action that I'd added, It was very flaky. Should investigate Firebase more.


#### Comment #3 by @iamralpht on 2023-06-29T18:28:41Z

Triage: Internal design doc authored for getting Firebase working with the automotive-design-compose github project. Moving from triage to backlog because we want a better way to identify changes that touch the serialization format before they're submitted.


#### Comment #4 by @timothyfroehlich on 2024-06-20T20:30:41Z

Need a better description


---

## <a id="issue-97"></a>Issue #97: Create method for setting the Figma Token once per device

- **URL:** [https://github.com/google/automotive-design-compose/issues/97](https://github.com/google/automotive-design-compose/issues/97)
- **Author:** @timothyfroehlich
- **Created:** 2023-05-31T21:30:59Z
- **Updated:** 2026-03-17T16:38:30Z
- **Labels:** `Type: New Feature`, `qol`, `:blue_square:  P3`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (1)

#### Comment #1 by @timothyfroehlich on 2024-06-20T20:28:44Z

Needs better description


---

## <a id="issue-101"></a>Issue #101: Automatically stage Release Candidates to gMaven

- **URL:** [https://github.com/google/automotive-design-compose/issues/101](https://github.com/google/automotive-design-compose/issues/101)
- **Author:** @timothyfroehlich
- **Created:** 2023-05-31T21:45:07Z
- **Updated:** 2026-03-17T16:38:25Z
- **Labels:** `engProd`, `:green_circle:  P2`, `Type: Task`
- **Assignees:** *None*

### Description

The current procedure is:

- Push a commit to the stable branch updating the release candidate number
- Cut the release on GitHub, wait for the m2repo build action to finish
- Download the finished artifact, stage on x20
- Run the gmaven_publish command to stage it.

This can all be done automatically via Kokoro


### Comments (1)

#### Comment #1 by @iamralpht on 2023-06-29T18:08:22Z

Pending internal review for Google backend components, moving out to 0.22.


---

## <a id="issue-103"></a>Issue #103: Allow for removal of Live Update code from production app

- **URL:** [https://github.com/google/automotive-design-compose/issues/103](https://github.com/google/automotive-design-compose/issues/103)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-01T16:18:52Z
- **Updated:** 2023-06-21T21:08:52Z
- **Labels:** `Type: New Feature`, `needs design`, `:blue_square:  P3`
- **Assignees:** *None*

### Description

A production DesignCompose app should not have the LiveUpdate code inside it. At best the code would be dead code which increases the size of a non-minified app by as much as 100MB and at worst it's an attack vector.

The ideal solution would be to separate the code out into a separate library that can be added or removed from the app. This will take some significant refactoring of the DesignCompose code.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-112"></a>Issue #112: "Favorite docs" - Add method for setting alternate Figma Files to load in the Design Switcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/112](https://github.com/google/automotive-design-compose/issues/112)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-06T21:22:09Z
- **Updated:** 2023-06-21T21:09:07Z
- **Labels:** `needs design`, `Type: Customer Issue`, `:blue_square:  P3`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (1)

#### Comment #1 by @iamralpht on 2023-06-08T16:51:13Z

Umesh notes that the existing feature to show branches allows easier navigation between docs in the Design Switcher, without needing to rebuild the app (so we can provide this guidance).


---

## <a id="issue-120"></a>Issue #120: Re-make doc page for Custom Fonts

- **URL:** [https://github.com/google/automotive-design-compose/issues/120](https://github.com/google/automotive-design-compose/issues/120)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-13T17:49:34Z
- **Updated:** 2023-06-21T21:09:34Z
- **Labels:** `documentation`, `:blue_square:  P3`
- **Assignees:** *None*

### Description

b/250922834


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-121"></a>Issue #121: Re-make doc page on Figma Interactions

- **URL:** [https://github.com/google/automotive-design-compose/issues/121](https://github.com/google/automotive-design-compose/issues/121)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-13T17:50:05Z
- **Updated:** 2023-06-21T21:09:30Z
- **Labels:** `documentation`, `:blue_square:  P3`
- **Assignees:** *None*

### Description

b/250921446


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-131"></a>Issue #131: Add documentation covering Dials & Gauges for developers

- **URL:** [https://github.com/google/automotive-design-compose/issues/131](https://github.com/google/automotive-design-compose/issues/131)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-22T17:04:15Z
- **Updated:** 2023-09-13T20:26:57Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** @rylin8

### Description

Dials & Gauges is now in the published plugin.

_Originally posted by @rylin8 in https://github.com/google/automotive-design-compose/pull/127#discussion_r1237999403_
         

File: docs/_docs/live-update/index.md


### Comments (2)

#### Comment #1 by @iamralpht on 2023-06-29T18:32:09Z

We have initial documentation here: https://google.github.io/automotive-design-compose/docs/modifiers/modifiers#dials-gauges-and-progress-bars-dials-gauges -- might need some embellishment and enhancement


#### Comment #2 by @iamralpht on 2023-08-02T20:14:18Z

We're discussing some design updates to the plugin, so let's wait on this until those have landed to avoid needing to immediately update the docs.


---

## <a id="issue-132"></a>Issue #132: Reorganize docs into a new set of categories

- **URL:** [https://github.com/google/automotive-design-compose/issues/132](https://github.com/google/automotive-design-compose/issues/132)
- **Author:** @timothyfroehlich
- **Created:** 2023-06-22T17:23:22Z
- **Updated:** 2023-06-29T18:48:09Z
- **Labels:** `documentation`, `:green_circle:  P2`
- **Assignees:** @miriamvu

### Description

I think we've got some good improvement opportunities for organizing the docs; maybe in round 2 we can make some new categories:

 - Intro (Intro, Getting Started, Capabilities, Tutorial)
 - Developing with DesignCompose (Interactions, Modifiers, Fonts)
 - Reference Apps (MediaCompose)
 - Using the Source (as we currently have it, maybe plus a doc on contributing)

@miriamvu also has some notes on content that would help answer some common questions (like we don't have a diagram showing the conversion process, serialized file, etc, and that info is important to understand to know how DC is different from code generation systems, and why it's good for automotive).,

_Originally posted by @iamralpht in https://github.com/google/automotive-design-compose/issues/127#issuecomment-1601988514_


### Comments (2)

#### Comment #1 by @timothyfroehlich on 2023-06-22T17:24:02Z

We also need a landing page for our docs, right now the Getting Started page is serving as that.


#### Comment #2 by @iamralpht on 2023-06-29T18:37:42Z

Assigning to @miriamvu for feedback and to figure out what's going to be most useful for partners.


---

## <a id="issue-136"></a>Issue #136: Impossible to fetch node under intersection/union

- **URL:** [https://github.com/google/automotive-design-compose/issues/136](https://github.com/google/automotive-design-compose/issues/136)
- **Author:** @elouannf
- **Created:** 2023-06-23T13:52:30Z
- **Updated:** 2024-05-08T20:48:43Z
- **Labels:** `needs design`, `:blue_square:  P3`
- **Assignees:** @iamralpht

### Description

I tried to reach a node which is used under an intersection in figma. When I tried to fetch it through DesignCompose i couldn't access to it. For example i used the element Modifier and I couln't modifiy my element. Here is a example : 
![image](https://github.com/google/automotive-design-compose/assets/131498280/8e4f3cc4-7fa4-4297-9a8e-f57e920a1fb6)


### Comments (11)

#### Comment #1 by @iamralpht on 2023-06-23T20:49:40Z

Yes, confirming that this is an issue -- children of unions/intersections are always treated as vectors, so the fact that there's a text node is lost. Let's keep this issue to fix customization inside of a union/intersection.

@elouannf we're adding support for mask layers in (https://github.com/google/automotive-design-compose/pull/85)[this change], which will be in the next release (0.21). Would masking let you achieve the effect as a work around?


#### Comment #2 by @elouannf on 2023-06-24T10:16:12Z

If masks are added I can use them instead of intersection.  If I can modify the mask thanks to a gauge/ modifier / text element  and acess to nodes inside the mask, the Design should work properly. Thank you for the support can wait to try it out !


#### Comment #3 by @iamralpht on 2023-06-29T18:40:30Z

@elouannf we're planning on cutting the RC for 0.21 tomorrow, so you should be able to try that out next week. Alternatively since mask layer support merged into main, you can also checkout main and build everything from source to get an earlier look at the functionality.


#### Comment #4 by @iamralpht on 2023-06-29T18:41:04Z

@timothyfroehlich please comment once RC is available


#### Comment #5 by @iamralpht on 2023-07-05T20:37:43Z

moving to iteration 0.22 so we keep track of the ticket


#### Comment #6 by @timothyfroehlich on 2023-07-14T16:51:04Z

Leaving for vacation distracted me from updating this ticket. RC's for 0.21.0 are available now, and we're wrapping up the full release of 0.21.0 now, which will be available via Maven by the end of the day.


#### Comment #7 by @elouannf on 2023-07-18T12:09:58Z

Hi we tried the new release and we found that there is still some trouble with masks ( for instance using Vectors as a mask). are there other limitations ?


#### Comment #8 by @rylin8 on 2023-07-18T15:36:37Z

Vectors as a mask should be supported. Could you provide an example of a mask setup that isn't working for you? Here are a couple screenshots of masks that we use in our test:
![Screenshot 2023-07-18 at 8 31 49 AM](https://github.com/google/automotive-design-compose/assets/130500397/35663a5c-33bc-4068-b3fa-c31089822833)
![Screenshot 2023-07-18 at 8 33 15 AM](https://github.com/google/automotive-design-compose/assets/130500397/44861c7a-9f7a-4080-bd7a-c59a838c0a1a)


#### Comment #9 by @iamralpht on 2023-08-09T20:21:17Z

@elouannf friendly ping on this -- do you have a mask example you can share that isn't working?


#### Comment #10 by @elouannf on 2023-08-14T11:17:21Z

Hi Ralph, sorry for the delay I just found the problem for our masks. It's not a big deal because I was able to make it work in another way. But here is the hierarchy that is currently working on figma but not on on DesignCompose it's not very important because I was able to make these changes anyway :
![image](https://github.com/google/automotive-design-compose/assets/131498280/10b1f521-78b7-4a25-94b0-2e955104ab54)
also this one is working in both : 
![image](https://github.com/google/automotive-design-compose/assets/131498280/7f0de72e-572a-43d3-b5c2-b4a9fb8e172f)


#### Comment #11 by @elouannf on 2023-08-17T14:58:22Z

[Untitled.zip](https://github.com/google/automotive-design-compose/files/12370866/Untitled.zip)
Hi Ralph , Here is an example. Even if there is an easier way to  create this animation at first and it didn't worked.  I tried to animate using your dial and gauge plugin.


---

## <a id="issue-177"></a>Issue #177: Implement lightweight theming using Variables

- **URL:** [https://github.com/google/automotive-design-compose/issues/177](https://github.com/google/automotive-design-compose/issues/177)
- **Author:** @iamralpht
- **Created:** 2023-07-12T20:17:57Z
- **Updated:** 2023-09-13T20:26:29Z
- **Labels:** `Type: New Feature`, `:exclamation: P1`
- **Assignees:** @rylin8

### Description

*No description provided.*


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-225"></a>Issue #225: Live update stops working with frames containing large number of layers

- **URL:** [https://github.com/google/automotive-design-compose/issues/225](https://github.com/google/automotive-design-compose/issues/225)
- **Author:** @FruitSnack1
- **Created:** 2023-07-18T06:45:04Z
- **Updated:** 2024-05-29T20:48:49Z
- **Labels:** `Type: Customer Issue`, `:exclamation: P1`
- **Assignees:** @iamralpht

### Description

When I paste a frame with 389 layers into document, live updates stops working. Frame contains only vectors so file size shouldn't be an issue. 

Restarting the application doesn't seem to help. No errors in Logcat either. Design switcher status shows old version of document. 

I got it to update once, but after that Live update stopped again.


### Comments (10)

#### Comment #1 by @rylin8 on 2023-07-19T20:29:50Z

Hi there. Sometimes this happens when something in the 389 layers has some kind of newer Figma property set that we don't yet support yet and fail to parse, causing deserialization of the Figma file to fail. Does the DesignSwitcher show any errors if you open it up? This is the little arrow at the top right that shows some live update info if you open it up like this:
![Screenshot 2023-07-19 at 1 25 14 PM](https://github.com/google/automotive-design-compose/assets/130500397/84e0a3f1-2afd-4d5c-b8b1-189ee63dabe3)

If you don't see anything there, is it possible to share a Figma document with the layers that cause this problem? Alternatively you could try taking subsets of those 389 layers to narrow down which part causes the problem.


#### Comment #2 by @FruitSnack1 on 2023-07-20T10:22:52Z

Hi. No errors in DesignSwitcher.
![image](https://github.com/google/automotive-design-compose/assets/28844239/bbba80f0-56a2-46b1-8f74-aad09fe26a49)
But this time I got some errors in logcat.
![image](https://github.com/google/automotive-design-compose/assets/28844239/232c1f12-aa7c-4577-af65-330981c95df4)
Seems like some components are not accessable. Can this be because they are from shared file ?
Unfortunately I am unable to share the document because of NDA.


#### Comment #3 by @rylin8 on 2023-07-21T17:37:37Z

Those errors mean that those nodes in your Figma documents are instances of components defined in other files. When DesignCompose comes across those types of nodes, it tries to download the full component from the other file, but sometimes fails for some reason (no access, original component has been deleted, etc). That's what look like has happened here. However, it usually does not cause an issue. The issue is logged to logcat, but it continues with the rest of the Figma document and renders what it can, so I don't think that is the issue here.

From what I see here, live update is still working. I don't know what is in all those layers you are copying, but without being able to see it I would suggest trying to break it down into smaller parts to see if it's a particular layer causing a problem. If you're able to reproduce this with a set of layers in a test doc not under NDA it would also be helpful and I can take a look.


#### Comment #4 by @FruitSnack1 on 2023-08-01T08:59:57Z

I tried to figure out what could be causing this issue. And found out, that Live update works fine, but the **fetch/parse takes too long** (~4 min for component containing 3 icons).

![image](https://github.com/google/automotive-design-compose/assets/28844239/16a4fc46-d835-4c97-8c0e-70694d353b6c)
 
![Frame 2609299 (1)](https://github.com/google/automotive-design-compose/assets/28844239/81f6bafe-cf78-441b-80d4-ecf4b853ab1d)
*_images are blurred because of NDA_

The component its fetching has 3 visible icons and another 8 hidden. All of them have 5 different variants. All of these components also have to be fetched from different file.

When I inserted a whole screen containing 389 layers the fetch/parse took so long I though that the Live update was broken.


#### Comment #5 by @timothyfroehlich on 2023-08-01T15:40:05Z

Thank you @FruitSnack1 ! Can you confirm which version of DesignCompose your app is using right now? We had an early release that left excessive logging enabled that caused significant fetch delays.


#### Comment #6 by @FruitSnack1 on 2023-08-03T11:06:53Z

Hi,
The version is v0.21.0


#### Comment #7 by @iamralpht on 2023-08-09T20:16:58Z

@FruitSnack1 I think this could also be caused by components referenced from the layers being in other documents. In that case, DesignCompose fetches the referenced documents and also processes those in order to extract the full components (in case they have variants, etc).

We're going to add some logging to surface things that could take a long time during fetch (#281), so hopefully in a future release you'll be able to repro and we can find out what to do to make a fix.


#### Comment #8 by @FruitSnack1 on 2023-08-10T08:58:55Z

Just figured out a quick solution.

In Figma you can **detach all instances** (turn components into frames, shapes, ...). 

Without all the components and variants from different files, live update works normal and updates in matter of seconds, event with frames containing hundreds of layers.

**To "detach all instances"**:
1. Select all elements you want to detach
2. Press `Ctrl` + `/`   (`Command` + `/` on mac)
3. Run the "Detach all nested instances" command

PS: Command should be ran in different file to avoid fetching all the components.


#### Comment #9 by @iamralpht on 2023-08-10T17:15:23Z

Ok, excellent. Are you using Figma's Design Library feature? We're wondering if we should restrict recursive doc fetching only to situations where a Design Library is in use (which would also limit the number of docs that must be fetched).


#### Comment #10 by @FruitSnack1 on 2023-08-11T11:32:36Z

Yes we do.


---

## <a id="issue-250"></a>Issue #250: Replace all references to Figma "documents" with "file"in the source code / API

- **URL:** [https://github.com/google/automotive-design-compose/issues/250](https://github.com/google/automotive-design-compose/issues/250)
- **Author:** @timothyfroehlich
- **Created:** 2023-07-28T14:53:42Z
- **Updated:** 2023-08-09T20:18:24Z
- **Labels:** `:blue_square:  P3`
- **Assignees:** *None*

### Description

Figma refers to the files as "files" in all [places](https://help.figma.com/hc/en-us/articles/1500005554982-Guide-to-files-and-projects), including in the URL for a file. As best I can tell, "document" is only used in the [REST API for a type of node within a file.](https://www.figma.com/developers/api#files). "Document" isn't used anywhere else in [their documentation](https://help.figma.com/hc/en-us/search?utf8=%E2%9C%93&query=document).

We should update our terminology to match this. The primary reason is simply for clarity in communication, removing ambiguity about whether our "document" is different from a Figma "file", and removing an extra "concept" from our documentation. 

If we make this change then we need to do it before releasing a 1.0. We should also provide a deprecation - migration path from the old names to the new ones so that we don't force an immediate upgrade.

Counterpoints:
- We don't want to tie ourselves too tightly to Figma: 
  - I'd say that File is generic enough that this would tie ourselves any further.
- The "document" as handled in the SDK is a processed and therefore different thing than the original File
  - This is an implementation detail that shouldn't be surfaced in the API or documentation


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-268"></a>Issue #268: Add Fetcher support for figma token file

- **URL:** [https://github.com/google/automotive-design-compose/issues/268](https://github.com/google/automotive-design-compose/issues/268)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:47:43Z
- **Updated:** 2026-03-17T16:37:44Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Similar to #239


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-269"></a>Issue #269: Manifest support for Fetcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/269](https://github.com/google/automotive-design-compose/issues/269)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:49:18Z
- **Updated:** 2026-03-17T16:37:42Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Add the manifest input options to the fetcher binary, including fetching multiple documents.

Changes should be confined to the figma_import crate.

Tests: Functional tests of the Fetcher.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-270"></a>Issue #270: Convert existing Kotlin JSON generation in Common and DesignCompose to use kotlinx.serialization

- **URL:** [https://github.com/google/automotive-design-compose/issues/270](https://github.com/google/automotive-design-compose/issues/270)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:52:34Z
- **Updated:** 2026-03-17T16:37:40Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Convert existing Kotlin [DocumentServerParams](https://github.com/google/automotive-design-compose/blob/wb/froeht/markdownlint/common/src/main/java/com/android/designcompose/common/DocumentServerParams.kt#L0-L1) [constructPostJson](https://github.com/google/automotive-design-compose/blob/wb/froeht/markdownlint/designcompose/src/main/java/com/android/designcompose/DocFetcher.kt#L25) to be generated a combined ManifestEntry data class. Create test ManifestEntry JSON.
Add validation mode to fetcher binary.

Tests: Add simple tests for serialization (Kotlin) and deserialization (Rust) using test manifests
Can generate them by outputting the JSON that is currently being generated by the Validation app.
This will simplify reviews of changes to the JSON schema.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-271"></a>Issue #271: Update Kotlin and Rust to new Manifest schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/271](https://github.com/google/automotive-design-compose/issues/271)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:53:39Z
- **Updated:** 2026-04-09T23:14:01Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Add the Manifest type, update ManifestEntry to match the final schema. Make changes in both Kotlin and Rust in the same PR

Tests added in #270 should still pass, add a test for a Manifest with multiple ManifestEntries. Update test manifests. All test manifests should have a Manifest as the root, with one or more ManifestEntries


### Comments (1)

#### Comment #1 by @anshu10sep on 2026-04-09T23:14:01Z

Tim, can you please add more details whats needed in here


---

## <a id="issue-272"></a>Issue #272: Incremental update support for Fetcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/272](https://github.com/google/automotive-design-compose/issues/272)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:54:17Z
- **Updated:** 2026-03-17T16:37:33Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

Add support for checking for existing files in the output folder and requesting updates appropriately

Tests: Unit tests using a test dataset of serialized files and ensuring the correct fetches would be performed.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-273"></a>Issue #273: Create Gradle figmaFileFetch task, including bundling the Fetcher tool with the plugin.

- **URL:** [https://github.com/google/automotive-design-compose/issues/273](https://github.com/google/automotive-design-compose/issues/273)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:58:09Z
- **Updated:** 2026-03-17T16:37:30Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Just the task, including command line support. This will include checking for existing serialized files and setting the last_modified field, image session and so on.

Tests: Gradle functional tests using [projectBuilder](https://docs.gradle.org/current/javadoc/org/gradle/testfixtures/ProjectBuilder.html), an integration test of the full packaging of the binary into the plugin using [TestKit](https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html).


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-274"></a>Issue #274: Connect the figmaFileFetch task to the output of the Codegen task, add DSL, update documentation.

- **URL:** [https://github.com/google/automotive-design-compose/issues/274](https://github.com/google/automotive-design-compose/issues/274)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:58:52Z
- **Updated:** 2026-03-17T16:37:28Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Enables the full pipeline for users from, @DesignDoc annotation to fetching the files and including the serialized files in the app.

Tests: Integration tests which run a live Figma fetch and run AndroidIntegrationTests to confirm that the file is deserialized and displays properly


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-275"></a>Issue #275: Create figmaFileCommit and figmaFileFetch andCommit tasks, create developer script to update all serialized files in the repository.

- **URL:** [https://github.com/google/automotive-design-compose/issues/275](https://github.com/google/automotive-design-compose/issues/275)
- **Author:** @timothyfroehlich
- **Created:** 2023-08-01T19:59:28Z
- **Updated:** 2026-03-17T16:37:26Z
- **Labels:** `:exclamation: P1`
- **Assignees:** *None*

### Description

Also update documentation to document the two new tasks.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-278"></a>Issue #278: Document using DesignCompose to create a traditional Android View

- **URL:** [https://github.com/google/automotive-design-compose/issues/278](https://github.com/google/automotive-design-compose/issues/278)
- **Author:** @iamralpht
- **Created:** 2023-08-02T20:20:55Z
- **Updated:** 2024-05-08T20:49:04Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** @iamralpht

### Description

All of our current examples and documentation cover using DC for full applications, but sometimes it's useful to be able to create individual views (e.g.: for IMEs and other system components).


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-282"></a>Issue #282: Consider making an allowlist for recursive doc fetches

- **URL:** [https://github.com/google/automotive-design-compose/issues/282](https://github.com/google/automotive-design-compose/issues/282)
- **Author:** @iamralpht
- **Created:** 2023-08-09T20:58:20Z
- **Updated:** 2023-08-23T20:11:10Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

In #167 we have a case where we're trying to fetch docs referenced from components that are only accidentally referenced via copy-paste in Figma. Figma makes it really easy to introduce these kinds of links, but we really only want to do the recursive fetches when the component is coming from a Design System.

Maybe we want to make an allow list for documents that we'll do recursive fetches from? Then you could opt in Design System docs, and we'd recursively fetch those, but not others.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-290"></a>Issue #290: Cleanup rectangle / bounding box setup

- **URL:** [https://github.com/google/automotive-design-compose/issues/290](https://github.com/google/automotive-design-compose/issues/290)
- **Author:** @kportl
- **Created:** 2023-08-15T17:16:48Z
- **Updated:** 2023-08-23T20:11:32Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** @srahman80

### Description

it's to remove this field: https://github.com/google/automotive-design-compose/blob/7e31e112d388e6f0b3151b70f2255324f9e6bd17/crates/figma_import/src/toolkit_schema.rs#L126

See internal ticket http://b/280682392


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-395"></a>Issue #395: Layout is broken on autolayout frames that are rotated

- **URL:** [https://github.com/google/automotive-design-compose/issues/395](https://github.com/google/automotive-design-compose/issues/395)
- **Author:** @rylin8
- **Created:** 2023-09-28T22:00:33Z
- **Updated:** 2023-11-01T20:22:51Z
- **Labels:** `:blue_square:  P3`, `layout`
- **Assignees:** @rylin8

### Description

![Screenshot 2023-09-28 at 2 59 09 PM](https://github.com/google/automotive-design-compose/assets/130500397/4c74dba1-e265-4e81-b58e-dbe0f663c3c9)


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-398"></a>Issue #398: Text alignment with multiple lines is incorrect

- **URL:** [https://github.com/google/automotive-design-compose/issues/398](https://github.com/google/automotive-design-compose/issues/398)
- **Author:** @rylin8
- **Created:** 2023-09-28T22:09:56Z
- **Updated:** 2023-11-14T21:47:34Z
- **Labels:** `:green_circle:  P2`, `layout`
- **Assignees:** @rylin8

### Description

Check the alignment Validation doc for details. Also, support auto width with multiple lines.


### Comments (1)

#### Comment #1 by @iamralpht on 2023-11-14T21:47:16Z

This is fairly close (about as accurate as before) with the most recent round of text optimization changes. Moving to P2.


---

## <a id="issue-469"></a>Issue #469: Separate the JNI crate into Live Update and Layout

- **URL:** [https://github.com/google/automotive-design-compose/issues/469](https://github.com/google/automotive-design-compose/issues/469)
- **Author:** @timothyfroehlich
- **Created:** 2023-10-16T15:54:23Z
- **Updated:** 2023-11-14T21:49:17Z
- **Labels:** `:green_circle:  P2`, `layout`
- **Assignees:** @rylin8

### Description

We need to have the layout code separate from the Live Update code so a production app can be released without Live Update. We need to write up a design for how we'll do this, but I believe the way that this will work will be to have a separate Live Update Android library, which will include and call it's own Live Update JNI rust library. The easiest way to accomplish that will be to split them into separate crates, IMO. 

We should talk about this more and come up with a plan.


### Comments (1)

#### Comment #1 by @iamralpht on 2023-11-14T21:49:17Z

Moving to P2 because this is a production support task, and shouldn't block the merge of `feature/rustlayout`.


---

## <a id="issue-551"></a>Issue #551: DesignCompose does not download images larger than a certain size

- **URL:** [https://github.com/google/automotive-design-compose/issues/551](https://github.com/google/automotive-design-compose/issues/551)
- **Author:** @rylin8
- **Created:** 2023-11-13T18:21:27Z
- **Updated:** 2024-01-10T21:46:01Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

Check for some kind of download limit from Figma on large images. If large images are not supported, we should be reporting some kind of error at least. Taken from https://b.corp.google.com/issues/310338161


### Comments (1)

#### Comment #1 by @timothyfroehlich on 2024-01-10T21:46:00Z

We should add an error.


---

## <a id="issue-561"></a>Issue #561: Performance optimization for Rust layout implementation

- **URL:** [https://github.com/google/automotive-design-compose/issues/561](https://github.com/google/automotive-design-compose/issues/561)
- **Author:** @iamralpht
- **Created:** 2023-11-14T21:52:57Z
- **Updated:** 2024-01-02T21:45:07Z
- **Labels:** `:green_circle:  P2`, `layout`
- **Assignees:** @rylin8

### Description

Performance, especially of list views, is currently regressed with the Rust Layout changes; we should profile and optimize again once we've got the functional pieces complete.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-573"></a>Issue #573: Make Brush function customization accept an `ImageContext`

- **URL:** [https://github.com/google/automotive-design-compose/issues/573](https://github.com/google/automotive-design-compose/issues/573)
- **Author:** @iamralpht
- **Created:** 2023-11-21T19:36:30Z
- **Updated:** 2023-11-21T19:36:30Z
- **Labels:** `Type: New Feature`, `:green_circle:  P2`, `java`
- **Assignees:** @iamralpht

### Description

If we passed `ImageContext` to the new `Brush` function customization, then an engineer could use a designer-supplied color in their shader, making a more generic solution.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-604"></a>Issue #604: Add support for baseline alignment

- **URL:** [https://github.com/google/automotive-design-compose/issues/604](https://github.com/google/automotive-design-compose/issues/604)
- **Author:** @iamralpht
- **Created:** 2023-12-11T19:17:19Z
- **Updated:** 2023-12-11T19:17:19Z
- **Labels:** `:green_circle:  P2`, `layout`
- **Assignees:** @rylin8

### Description

It looks like we can add this to `feature/rustlayout`, and it's exposed in Figma, and commonly used by designers.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-606"></a>Issue #606: Investigate transition to Kotlin 2.0 and KSP 2.0

- **URL:** [https://github.com/google/automotive-design-compose/issues/606](https://github.com/google/automotive-design-compose/issues/606)
- **Author:** @timothyfroehlich
- **Created:** 2023-12-12T22:31:09Z
- **Updated:** 2026-03-17T16:36:38Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

https://android-developers.googleblog.com/2023/12/ksp2-preview-kotlin-k2-standalone.html


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-628"></a>Issue #628: Galaxy design crashes emulators on APIs greater than 33

- **URL:** [https://github.com/google/automotive-design-compose/issues/628](https://github.com/google/automotive-design-compose/issues/628)
- **Author:** @timothyfroehlich
- **Created:** 2023-12-13T21:52:28Z
- **Updated:** 2026-03-17T16:36:35Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (1)

#### Comment #1 by @timothyfroehlich on 2024-01-10T21:50:42Z

The Shiny design was most consistent in causing a crash.

May have fixed, attempt to reproduce


---

## <a id="issue-635"></a>Issue #635: Allow `fetch` to get its nodes list from the JSON emitted by the KSP processor

- **URL:** [https://github.com/google/automotive-design-compose/issues/635](https://github.com/google/automotive-design-compose/issues/635)
- **Author:** @iamralpht
- **Created:** 2023-12-14T09:27:03Z
- **Updated:** 2026-03-17T16:36:33Z
- **Labels:** `Type: New Feature`, `qol`, `engProd`, `:green_circle:  P2`
- **Assignees:** *None*

### Description

Partners are using the `fetch` tool for some offline workflows, and would like to have it do exactly what DC does at runtime (without needing to manually specify all of the nodes on the command line). Perhaps we could make `fetch` use the JSON (or a new JSON) emitted by the KSP processor so that it fetches in the exact same way as DesignCompose at runtime?


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-649"></a>Issue #649: Provide a means for users to access the Validation docs

- **URL:** [https://github.com/google/automotive-design-compose/issues/649](https://github.com/google/automotive-design-compose/issues/649)
- **Author:** @timothyfroehlich
- **Created:** 2023-12-19T17:36:03Z
- **Updated:** 2025-11-21T06:34:58Z
- **Labels:** `:blue_square:  P3`
- **Assignees:** @iamralpht

### Description

Currently the Validation docs are only available to Googlers. They could be used as example Figma designs, but no one outside of Google can access them.  I believe the designs should be publicly available (read-only).


### Comments (3)

#### Comment #1 by @timothyfroehlich on 2024-06-03T14:02:26Z

We now have the ability to do this! We have a new "team" in Figma which has permissions to set the access on files to public/read-only. Need to discuss how to move forward on this.


#### Comment #2 by @williamliuok on 2025-11-21T06:32:08Z

I’d like to express my strong support for making the Figma design files publicly accessible as read-only. Having these resources available would be incredibly valuable for the broader community, especially for those of us actively using and contributing to this framework.

The ability to reference the official design examples would greatly improve our workflow, ensure consistency in implementation, and help align community contributions with the intended design direction. I understand there may be processes to discuss, but I truly hope this can be prioritized and moved forward soon.


#### Comment #3 by @superguoshiyu on 2025-11-21T06:34:58Z

How can we get these docs, and we want to use this tool in our company to improve our work effeciency.


---

## <a id="issue-661"></a>Issue #661: Improve documentation on finding and using the generated JSON file for Lists & Grids

- **URL:** [https://github.com/google/automotive-design-compose/issues/661](https://github.com/google/automotive-design-compose/issues/661)
- **Author:** @iamralpht
- **Created:** 2023-12-21T22:35:52Z
- **Updated:** 2024-01-03T21:05:35Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

We mention the generated JSON file here: https://google.github.io/automotive-design-compose/docs/modifiers/modifiers#creating-lists -- but we're quite vague in how to find it and upload it.

This is a critical step and also enables the error checking plugin; we should have a dedicated page on this topic.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-673"></a>Issue #673: Support scale constraints with vectors

- **URL:** [https://github.com/google/automotive-design-compose/issues/673](https://github.com/google/automotive-design-compose/issues/673)
- **Author:** @rylin8
- **Created:** 2023-12-28T23:18:17Z
- **Updated:** 2024-01-02T21:43:04Z
- **Labels:** `:blue_square:  P3`, `layout`
- **Assignees:** *None*

### Description

Currently we only support scale constraints for frames and rectangle vectors. Arbitrarily shaped vectors that follow a path stay a fixed size when their constraints are set to scale. The first pass at fixing this caused a lot of regressions, including many of the telltales being rendered at the wrong scale.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-684"></a>Issue #684: Configure Firebase Test Lab, add performance testing

- **URL:** [https://github.com/google/automotive-design-compose/issues/684](https://github.com/google/automotive-design-compose/issues/684)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-02T20:25:07Z
- **Updated:** 2026-03-17T16:36:20Z
- **Labels:** `engProd`, `:green_circle:  P2`
- **Assignees:** *None*

### Description

### Scope

This initial task will add post-submit performance testing of benchmarks to DesignCompose. The initial tests will use the existing Battleship benchmarks. The FTL configuration is essentially a different way of running the existing benchmarks, which can already be run on a local device.

### Rough Design

Tests will run with [Firebase Test Lab](https://firebase.google.com/docs/test-lab/android/android-studio) and be configured via [Gradle](https://developer.android.com/studio/test/gradle-managed-devices#gmd-ftl). This will require some kind of authentication with our new GCP project (details to come).

### Viewing results

Results will be recorded to our FTL project. I'll be exploring what options exist there. I intend to create a dashboard to track the results of the benchmarks over time.

### Adding additional benchmarks

This is out of scope for this task. Macro-benchmarks need to target specific use cases and require more specific knowledge of what to test. The feedback loop is tight enough that the same person who is making optimization should be writing the benchmarks to test those optimizations.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-688"></a>Issue #688: Nightly Experimental builds failing

- **URL:** [https://github.com/google/automotive-design-compose/issues/688](https://github.com/google/automotive-design-compose/issues/688)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-03T16:08:05Z
- **Updated:** 2024-01-03T16:08:05Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Recent runs of the [Nightly Experimental builds](https://github.com/google/automotive-design-compose/actions/runs/7392555776/job/20111197070?pr=686#step:11:2018) are failing due to the runner running out of space. This is probably related to merging MediaCompose into the main branch. There's likely a number of improvements I can make here.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-690"></a>Issue #690: Improve handling of AAOS Unbundled Libraries in GitHub Actions

- **URL:** [https://github.com/google/automotive-design-compose/issues/690](https://github.com/google/automotive-design-compose/issues/690)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-03T16:21:36Z
- **Updated:** 2024-01-03T19:41:48Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

The Unbundled libraries no longer need to be compiled in every CI run. Previously the MediaCompose project needed to include the Unbundled project in it's build and build the Unbundled libraries. Now the Unbundled project can compile the libraries into a Maven repository once for other builds to use.

Thanks to this, we can move the Unbundled library compilation into a separate workflow and export the resulting artifact for any workflow that needs them. The Unbundled workflow would only need to run once, essentially, to build the libraries and upload the artifact. 

This will significantly improve the main [sdk build](https://github.com/google/automotive-design-compose/actions/runs/7399540981/job/20131210074) (<- example) run times, as the SDK build won't need to wait for the Unbundled libraries to build.

This would be a good opportunity to do #658 as well, to migrate to use the upgraded GitHub artifacts actions


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-708"></a>Issue #708: Modify Tutorial instructions to run "setFigmaTokenDebug" from Android Studio instead of the command line.

- **URL:** [https://github.com/google/automotive-design-compose/issues/708](https://github.com/google/automotive-design-compose/issues/708)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T14:48:07Z
- **Updated:** 2024-01-10T14:52:28Z
- **Labels:** `top-qol`
- **Assignees:** *None*

### Description

This should simplify the tutorial and some issues we've had with new users.

Also, perhaps write the Figma Token's config file using Android Studio (use it to create the file and so-on).


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-709"></a>Issue #709: Enhance "SetFigmaToken" to target the actively running Android Device

- **URL:** [https://github.com/google/automotive-design-compose/issues/709](https://github.com/google/automotive-design-compose/issues/709)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T14:49:49Z
- **Updated:** 2024-01-10T14:51:40Z
- **Labels:** `top-qol`
- **Assignees:** *None*

### Description

Using the macros I learned about for getting the active device when running gradle commands from Android Studio, this should be fairly easy to implement.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-710"></a>Issue #710: Feature idea: Automatically add Compose test tags to all DesignCompose customizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/710](https://github.com/google/automotive-design-compose/issues/710)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:04:37Z
- **Updated:** 2026-03-17T16:36:11Z
- **Labels:** `top-qol`
- **Assignees:** *None*

### Description

Need to think through it a little more, but that would simplify writing tests.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-711"></a>Issue #711: Test library - Create examples of various tests

- **URL:** [https://github.com/google/automotive-design-compose/issues/711](https://github.com/google/automotive-design-compose/issues/711)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:35:15Z
- **Updated:** 2024-01-10T16:35:16Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Including (just to name a few):

- Confirm that a designDoc displays
- Confirm that variant is shown
- Confirm the value of a dial/gauge


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-712"></a>Issue #712: Documentation - Document compile-time optimizations

- **URL:** [https://github.com/google/automotive-design-compose/issues/712](https://github.com/google/automotive-design-compose/issues/712)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T16:45:19Z
- **Updated:** 2024-01-10T16:46:15Z
- **Labels:** `documentation`
- **Assignees:** *None*

### Description

Specifically separating the DesignDoc Interface declarations into their own file, building off of #452


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-713"></a>Issue #713: Tutorial - consider alternative instructions for loading your Figma Doc ID

- **URL:** [https://github.com/google/automotive-design-compose/issues/713](https://github.com/google/automotive-design-compose/issues/713)
- **Author:** @timothyfroehlich
- **Created:** 2024-01-10T17:50:14Z
- **Updated:** 2024-01-10T17:50:30Z
- **Labels:** `documentation`, `tutorial`
- **Assignees:** *None*

### Description

We're having consistent new user issues here, and they seem exacerbated when the user already has access to the original Tutorial Doc. We should change the instructions here. I'm leaning towards directly changing `reference-apps/tutorial/app/src/main/java/com/android/designcompose/tutorial/MainActivity.kt`.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-739"></a>Issue #739: Design Component must be within the main document Figma interface

- **URL:** [https://github.com/google/automotive-design-compose/issues/739](https://github.com/google/automotive-design-compose/issues/739)
- **Author:** @Heneveld
- **Created:** 2024-01-23T13:30:23Z
- **Updated:** 2024-06-07T17:48:18Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** @rylin8

### Description

When creating a reusable Design Component there seems to be no way to avoid adding the nodes declared for the interface to the main figma document.  When adding a separate interface it is required to add **CustomComponent** implementation or it will not compile. I've attached a project with a Design Component Indicator that is declared in interface **FigmaIndicator** which will compile but show up blank. If I move this to interface **ExampleDoc** it will work. I understand I could override this in the main **ExampleDoc** interface however that would require anyone using this component elsewhere would need to do the same.
[Ex4Google.zip](https://github.com/google/automotive-design-compose/files/14024563/Ex4Google.zip)


### Comments (2)

#### Comment #1 by @rylin8 on 2024-03-15T02:10:32Z

@Heneveld we have been discussing various ways to better support code reuse in DesignCompose and are planning to add some new features that I think would help with this issue. However, we are also leaning toward disallowing inheritance with @DesignDocument interfaces. Is there any particular reason that you need inheritance with this example? Could you instead declare two interfaces with the same document ID (though I admit this won't work yet)? For example:

```
@DesignDoc(id = "abc")
interface TestMain {
    @DesignComponent(node = "#stage") fun Main(
        @Design(node = "#replace-node") replaceNode: @Composable (ComponentReplacementContext) -> Unit,
    )
}

@DesignDoc(id = "abc")
interface TestNode {
    @DesignComponent(node = "#node") fun Node()
}

@Composable
fun Example() {
    TestMainDoc.Main(replaceNode = { TestNodeDoc.Node() })
}
```


#### Comment #2 by @Heneveld on 2024-03-15T11:38:31Z

Hi Rob,

I don’t see any issue with declaring separate interfaces if multiple interfaces can be assigned a document id. The only reason inheritance was being used was to provide the interface separate and still be allowed to assign Figma nodes in the current implementation.

I’m not sure if it would be necessary to have different document ids at this time as the main Figma document would already have an instance of the component if it’s being used.

Thanks,
Jonathan

From: Robert Lin ***@***.***>
Sent: Thursday, March 14, 2024 10:11 PM
To: google/automotive-design-compose ***@***.***>
Cc: Heneveld, Jonathan ***@***.***>; Mention ***@***.***>
Subject: [EXTERNAL] Re: [google/automotive-design-compose] Design Component must be within the main document Figma interface (Issue #739)

[EXTERNAL EMAIL]

@Heneveld<https://github.com/Heneveld> we have been discussing various ways to better support code reuse in DesignCompose and are planning to add some new features that I think would help with this issue. However, we are also leaning toward disallowing inheritance with @DesignDocument interfaces. Is there any particular reason that you need inheritance with this example? Could you instead declare two interfaces with the same document ID (though I admit this won't work yet)? For example:

@DesignDoc(id = "abc")

interface TestMain {

    @DesignComponent(node = "#stage") fun Main(

        @Design(node = "#replace-node") replaceNode: @Composable (ComponentReplacementContext) -> Unit,

    )

}



@DesignDoc(id = "abc")

interface TestNode {

    @DesignComponent(node = "#node") fun Node()

}



@Composable

fun Example() {

    TestMainDoc.Main(replaceNode = { TestNodeDoc.Node() })

}

—
Reply to this email directly, view it on GitHub<https://github.com/google/automotive-design-compose/issues/739#issuecomment-1998819468>, or unsubscribe<https://github.com/notifications/unsubscribe-auth/BA5CVJ32VEJ5GXOKQSANG6DYYJKC3AVCNFSM6AAAAABCG76UDOVHI2DSMVQWIX3LMV43OSLTON2WKQ3PNVWWK3TUHMYTSOJYHAYTSNBWHA>.
You are receiving this because you were mentioned.Message ID: ***@***.******@***.***>>


---

## <a id="issue-840"></a>Issue #840: Variant prototype not working when the elements come from a library

- **URL:** [https://github.com/google/automotive-design-compose/issues/840](https://github.com/google/automotive-design-compose/issues/840)
- **Author:** @MarcFerrerVWGS
- **Created:** 2024-03-04T08:26:33Z
- **Updated:** 2024-06-07T17:48:19Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Currently I am importing on my figma files some elements from my custom library. One of those is a button which has multiple variants. It has one state that allows the button to show as pressed. I added this variant on the prototype on figma on pressing action and it works, but when I loaded on android it doesn't work.

Also if I take the property from code it doesn't work. Could be possible that have the same name for a state in multiple buttons it don't work as expected? Because I have multiple buttons that come from my library button and all have the same variants state names on it.

To test that also I created a local button like the one I have on the library and did the same and then the button works on android simulation. 

This is the variants of my button from my library:
![image](https://github.com/google/automotive-design-compose/assets/156323011/70b95fcd-c45f-4a84-b9d6-875ef8575a9b)

This are the buttons I added on my stage:
![image](https://github.com/google/automotive-design-compose/assets/156323011/d76f2734-6f3e-439f-937e-0f2de7eb5c9c)

This is my interaction on the buttons to change the state while being pressed:
![image](https://github.com/google/automotive-design-compose/assets/156323011/5c106242-925c-4836-928c-24d9b89a8d9d)

Also I am getting this warning:
Unable to dispatch CHANGE_TO; missing instance id or destination id


### Comments (12)

#### Comment #1 by @MarcFerrerVWGS on 2024-03-04T11:32:47Z

I just checked and this only happens when the button/element comes from published library. When you add a prototype that changes the variant on figma works but on the emulator didn't and also seems that is not possible to get the variant value from code if it is comming from the published library.


#### Comment #2 by @timothyfroehlich on 2024-03-04T20:41:32Z

@iamralpht What do you think here?


#### Comment #3 by @timothyfroehlich on 2024-03-06T17:12:42Z

@MarcFerrerVWGS It looks like this might be the same as #691


#### Comment #4 by @MarcFerrerVWGS on 2024-03-07T07:21:18Z

Hi @timothyfroehlich, I saw the issue that you linked but the problem I am facing is when the component instanced comes from a external library the events from prototype don't work, for example the while pressing. 
Also I can get the variant from code and the component has de same name as the parent


#### Comment #5 by @MarcFerrerVWGS on 2024-03-07T09:14:42Z

In my code I am doing this:

 @DesignComponent(node = "#stage", isRoot = true)
    fun Stage(
        @DesignVariant(property = "#state") state: State
        .......
        state = State.pressed
        .......

And then I try to modify the state and it didn't show the change in the emulator and I only have 1 button instanced with the property #state that comes from 3r party component


#### Comment #6 by @iamralpht on 2024-03-08T09:30:38Z

I think there are two things you might be trying here:

  1. Using the interactions system to specify a "while pressed" variant
  2. Using an enum mapped to a variant to specify the variant to show

I can see (1) not working if the "Sync Interactions" plugin wasn't run in the library, but I think (2) should work in any case. Can you clarify which one you're trying?


#### Comment #7 by @MarcFerrerVWGS on 2024-03-08T11:18:51Z

(1) What I am doing is that my component from the library has the interaction "press" that changes the variant to have the effect pressed. When I import this component as instance on my #stage, if I run the flow/prototype I can see the variant change while I am pressing but when I import on the emulator and did the fetch with the live update option activated that behaviour didn't work. The button didnt' change the variant when I am pressing.
(2) I have an enum with the properties of the variant #state. In code I added a tapCallback to the button and when I tap I change the state to pressed (for example). The result is nothing happens but I put a print on the tapCallBack and the click works.


#### Comment #8 by @rylin8 on 2024-03-15T23:50:41Z

Hi @MarcFerrerVWGS, sorry for the delay.

For 1, could you verify that you are using syncing prototype changes with our plugin?
![Screenshot 2024-03-15 at 4 48 44 PM](https://github.com/google/automotive-design-compose/assets/130500397/4f1f2974-5ad1-4208-8153-8aaff0b130af)

For 2, there could a be a number of reasons that this is not working for you and it's difficult to tell without a more concrete example. Would you be able to provide some more complete sample code as well as the Figma file that goes with it?


#### Comment #9 by @MarcFerrerVWGS on 2024-03-18T06:17:36Z

Hi,
I ran de plugin everytime I change the values. I will try to sent you some sample code and figma if I can.


#### Comment #10 by @MarcFerrerMargarit on 2024-03-20T07:03:09Z

Hi, I added the main activity and all the figma files on this zip file. It has the main view and then the components and the libraries. If you can't download just tell me and I uploaded again.
https://drive.google.com/file/d/1KLWCChLN5FHqW-SCsHGjqysbJCok9y06/view?usp=sharing


#### Comment #11 by @rylin8 on 2024-03-20T21:17:41Z

I took a quick look and one thing I can suggest is to define enums and declare `@DesignVariant` parameters for the other properties on your components. For example one of the components I looked at had four properties, named "category", "configuration", "state", and "emphasis_level". Since there is only one `@DesignVariant` parameter for the "state" property, and there are multiple variants with the "state" set to "disabled", DesignCompose will now know which one to pick, so results can be unexpected.

If truly everything works correctly when the components are in the same file but fail when components are in a different file, and the Figma token used for live update has access to the other files, then this could be a bug on our end and we'll need to prioritize it with the rest of the bugs on our backlog.


#### Comment #12 by @MarcFerrerVWGS on 2024-03-21T06:27:23Z

I will take a look on what do you say. Anyway the files I attached before it seems to be not loading, I let the code running on the android emulator and it gets stuck on the fetching and it is not showing any error or anything. I let it 15 minutes and still on fetching. All this with the components in the other files.

If I add the component on the same file everything is fine.


---

## <a id="issue-883"></a>Issue #883: Consolidate JNI Library artifacts into a single Gradle module

- **URL:** [https://github.com/google/automotive-design-compose/issues/883](https://github.com/google/automotive-design-compose/issues/883)
- **Author:** @timothyfroehlich
- **Created:** 2024-03-19T18:40:40Z
- **Updated:** 2024-03-19T18:40:41Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

The JNI Library is now used in a number of ways. Android builds are included in the core DesignCompose library and host builds are being added via the new test-internal library. It's also required for Android Studio Compose previews, which are non-functional right now.

There's a significant amount of variations of the builds. Right now we're compiling 32-bit and 64 bit x86 and Arm Android builds, x86_64 Linux builds, with Debug and Release builds of each. Some platforms need different code, such as host builds (i.e. Robolectric Unit tests) needing to unpack and load the library, and right now those builds need to use the Android JVM (or Robolectric). And right now we only have support for building the x86_64 Linux host builds. We don't have a clean way to add MacOS or Windows builds.

Consolidating the JNI portion of the code into a separate module would reduce code debt and simplify the build. Use of Kotlin Multi-Platform would enable us to build the JNI code such that we could remove the reliance on an Android JVM/Robolectric and enable a path towards Compose previews. It would allow for a better framework for MacOS and Windows builds, and simplify the separation between the layout JNI and Live Update JNI code.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-890"></a>Issue #890: Use version catalog for exoplayer test dependency

- **URL:** [https://github.com/google/automotive-design-compose/issues/890](https://github.com/google/automotive-design-compose/issues/890)
- **Author:** @iamralpht
- **Created:** 2024-03-21T22:44:23Z
- **Updated:** 2024-03-22T15:29:02Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Please replace with version catalog entries

_Originally posted by @timothyfroehlich in https://github.com/google/automotive-design-compose/pull/859#discussion_r1532883887_


### Comments (1)

#### Comment #1 by @timothyfroehlich on 2024-03-22T15:29:01Z

It's pretty easy with a current Android Studio release, I can give you a demo.


---

## <a id="issue-905"></a>Issue #905: Masks inside of variants do not render correctly

- **URL:** [https://github.com/google/automotive-design-compose/issues/905](https://github.com/google/automotive-design-compose/issues/905)
- **Author:** @HooHaa-code
- **Created:** 2024-03-29T19:45:01Z
- **Updated:** 2024-06-07T17:48:20Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Having a mask inside of a variant causes the object to render incorrectly:

### Rendered
![image](https://github.com/google/automotive-design-compose/assets/64603080/b155faa7-d1bc-4297-b769-d457259d9fdf)

### Expected
![image](https://github.com/google/automotive-design-compose/assets/64603080/d4bb124f-3876-4488-96ed-f6fcb9cd65a4)

example project is attached
[Ex4Google-variant-mask.zip](https://github.com/google/automotive-design-compose/files/14808046/Ex4Google-variant-mask.zip)


### Comments (6)

#### Comment #1 by @iamralpht on 2024-04-01T05:37:59Z

@HooHaa-code is this with 0.22-rc or 0.21? I'm wondering if the layout changes improve matters.

Alternatively, there's a new implementation of masking in `main` if you enable "SQUOOSH" here: https://github.com/google/automotive-design-compose/blob/3d8f2e9f5e39ff32476e6d3ee3e576e9e612b339/designcompose/src/main/java/com/android/designcompose/DesignView.kt#L971 -- this will break all kinds of other things (lists and grids, content substitution, etc) but it improves some rendering fidelity, e.g.: multiple masks on a single object should work, some basic animations are present, etc.


#### Comment #2 by @HooHaa-code on 2024-04-01T14:09:10Z

I have '+' set for design compose in libs.versions.toml.  So it should be 0.25.0


#### Comment #3 by @iamralpht on 2024-04-01T17:16:44Z

Ok, I am not sure if that would have gotten you the 0.25.0-rc release (@timothyfroehlich knows how these rules work). We just pushed 0.25.0 to Maven (about 30 minutes ago) so updating dependencies now should definitely pull it in.


#### Comment #4 by @HooHaa-code on 2024-04-01T17:23:22Z

I guess I assumed that when a release it git hub is made that we would get it though maven.  I was wrong in that assumption.  I just did a sync and say the 0.25.0 artifacts get pulled in.

The behavior is still the same


#### Comment #5 by @timothyfroehlich on 2024-04-01T17:53:20Z

That was my mistake, sorry about that.after I create the release I need to
wait for the release job to finish building the artifact so that I can
upload to Google's maven repository. I lost track of things on Friday and
didn't complete the process.

We'll have to look into the masks issue

On Mon, Apr 1, 2024, 12:23 PM HooHaa-code ***@***.***> wrote:

> I guess I assumed that when a release it git hub is made that we would get
> it though maven. I was wrong in that assumption. I just did a sync and say
> the 0.25.0 artifacts get pulled in.
>
> The behavior is still the same
>
> —
> Reply to this email directly, view it on GitHub
> <https://github.com/google/automotive-design-compose/issues/905#issuecomment-2030197860>,
> or unsubscribe
> <https://github.com/notifications/unsubscribe-auth/ABMM2SSSKDPLERV4QLW6773Y3GJ2BAVCNFSM6AAAAABFO3ARXWVHI2DSMVQWIX3LMV43OSLTON2WKQ3PNVWWK3TUHMZDAMZQGE4TOOBWGA>
> .
> You are receiving this because you were mentioned.Message ID:
> ***@***.***>
>


#### Comment #6 by @rylin8 on 2024-05-06T16:26:59Z

FYI while testing out https://github.com/google/automotive-design-compose/issues/1056 I tried enabling squoosh and the mask appeared to render correctly, though component replacements did not work.


---

## <a id="issue-954"></a>Issue #954: Enable DesignModule usage across Gradle projects

- **URL:** [https://github.com/google/automotive-design-compose/issues/954](https://github.com/google/automotive-design-compose/issues/954)
- **Author:** @timothyfroehlich
- **Created:** 2024-04-03T16:19:39Z
- **Updated:** 2024-06-20T20:39:00Z
- **Labels:** `Type: New Feature`
- **Assignees:** *None*

### Description

This may take some work to figure out how to share the Module annotations across Gradle Projects. Ideally the solution would work for building and releasing a Maven artifact as well.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1031"></a>Issue #1031: Vectors in Serialized Documents Don't Provide all Colors

- **URL:** [https://github.com/google/automotive-design-compose/issues/1031](https://github.com/google/automotive-design-compose/issues/1031)
- **Author:** @johnoneil
- **Created:** 2024-04-23T04:27:36Z
- **Updated:** 2024-04-26T15:47:28Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

This is a little difficult to describe, so a picture is helpful:

![Screenshot from 2024-04-22 21-06-53](https://github.com/google/automotive-design-compose/assets/2950232/5d0ebebf-ce16-4e72-a772-9a19595e5f0a)

In the image above, the arrow is represented by a single Vector element (see the left column)
But there are two colors associated with this Vector.

Two colors are associated with this vector. See the right column. These are 68DB6D (green) and FFFFFF (white), for the outer circle and inner arrow respectively.

The problems arise in the serialized document result of the `fetch` tool`. In the serialized design document, the Vector itself is separated into two groups of points, one for the outer circle, and one for the inner arrow. But only one Color is present (white). We must be packing only the last one.

This is difficult to see outside the debugger but I was able to `trace!` our traversal through the serialized document:

```
DisplayList.rs visit ---> Node Name: #telltale/left-blinker
..
2024-04-23T04:23:44.703Z TRACE [harry::render_displaylist] There are 1 background colors provided for this vector path.
...
```

I'm assuming for a Vector with N paths, it should have (somewhere) N colors.

This is a simple case however, with only "fill" style paths. I think we might have more complex cases where there are both fills and strokes, so I think we may have to pack all the N fill colors and the M stroke colors in a different place, probably a dedicated struct for each.


### Comments (2)

#### Comment #1 by @rylin8 on 2024-04-23T18:02:28Z

Interesting find! Does that mean that the Kotlin renderer is also unable to draw this with both colors? I don't recall this happening, but maybe we also never used a Vector like this with multiple colors?


#### Comment #2 by @iamralpht on 2024-04-26T15:47:27Z

Ok, I found this node, and it's a vector network with mixed fills. To render this correctly, we need to add support for the `fillOverrideTable` and `overrideID` properties of vectors:

```json
            "fills": [
              {
                "blendMode": "NORMAL",
                "type": "SOLID",
                "color": {
                  "r": 1,
                  "g": 1,
                  "b": 1,
                  "a": 1
                }
              }
            ],
            "fillOverrideTable": {
              "1": {
                "fills": [
                  {
                    "blendMode": "NORMAL",
                    "type": "SOLID",
                    "color": {
                      "r": 0.4077083468437195,
                      "g": 0.8583333492279053,
                      "b": 0.4257333278656006,
                      "a": 1
                    }
                  }
                ]
              },
              "2": null
            },
            "fillGeometry": [
              {
                "path": "M0 38.5C0 17.237 17.237 0 38.5 0C59.763 0 77 17.237 77 38.5C77 59.763 59.763 77 38.5 77C17.237 77 0 59.763 0 38.5Z",
                "overrideID": 1,
                "windingRule": "NONZERO"
              },
              {
                "path": "M60.1562 38.4129L37.5803 19.25L37.5803 30.3993L21.6562 30.3993L21.6562 46.6007L37.5803 46.6007L37.5803 57.75L60.1562 38.4129Z",
                "windingRule": "NONZERO"
              }
            ],
```

So one path is using a fill override of 1, which corresponds to the green fill. We don't support that yet, so we'll get a white fill for the whole node.


---

## <a id="issue-1117"></a>Issue #1117: ImageContext does not provide useful data when node constraints set to scale

- **URL:** [https://github.com/google/automotive-design-compose/issues/1117](https://github.com/google/automotive-design-compose/issues/1117)
- **Author:** @rylin8
- **Created:** 2024-05-15T17:15:57Z
- **Updated:** 2024-05-15T17:16:14Z
- **Labels:** `:green_circle:  P2`
- **Assignees:** *None*

### Description

The `@Composable (ImageReplacementContext) -> Bitmap?` customization provides context for the node whose background will be replaced with an image. However if the node is set to scale constraints, the `ImageReplacementContext` data will not have useful size information for that node.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1118"></a>Issue #1118: Provide additional test cases for external renderer comparison.

- **URL:** [https://github.com/google/automotive-design-compose/issues/1118](https://github.com/google/automotive-design-compose/issues/1118)
- **Author:** @johnoneil
- **Created:** 2024-05-15T19:46:46Z
- **Updated:** 2024-05-15T19:46:46Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

This bug is primarily for tracking work to provide additional test images so an external renderer can do apples-to-apples comparisons between Figma, `automotive-design-compose` and renderer X.

Will associate current PR with this issue.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1138"></a>Issue #1138: Custom Design Modules must be in same directory as Generated Composable

- **URL:** [https://github.com/google/automotive-design-compose/issues/1138](https://github.com/google/automotive-design-compose/issues/1138)
- **Author:** @Heneveld
- **Created:** 2024-05-21T14:00:27Z
- **Updated:** 2024-06-07T17:47:55Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Fails to compile with other module as it cannot find the customizations but the root Module can be found.

![image](https://github.com/google/automotive-design-compose/assets/138029735/5a102a50-53ce-43f4-a6d1-d0999fbb23a2)
![image](https://github.com/google/automotive-design-compose/assets/138029735/66851850-2b92-4c74-873a-ecd038d8ad94)

[Ex4Google.zip](https://github.com/google/automotive-design-compose/files/15390885/Ex4Google.zip)


### Comments (1)

#### Comment #1 by @rylin8 on 2024-05-23T18:45:02Z

Hi @Heneveld, this is due to `OtherModules` being in a different package. If you change the package name to be the same as MainActivity, it should work. We'll look into supporting different package names.


---

## <a id="issue-1167"></a>Issue #1167: How to scale the Figma objects in the emulator based on the screen DPI

- **URL:** [https://github.com/google/automotive-design-compose/issues/1167](https://github.com/google/automotive-design-compose/issues/1167)
- **Author:** @spkdroid
- **Created:** 2024-05-28T15:16:48Z
- **Updated:** 2024-06-07T17:47:56Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

I want to scale the Figma object rendered on the emulator or the phone hardware based on the screen DPI. Is there a way to do this programmatically?


### Comments (1)

#### Comment #1 by @iamralpht on 2024-05-29T15:17:36Z

The objects are already scaled based on DPI, if you want to scale them more then you can use a Compose scale modifier outside of the Composable generated by DesignCompose.


---

## <a id="issue-1169"></a>Issue #1169: Support duplicate variable names across multiple documents

- **URL:** [https://github.com/google/automotive-design-compose/issues/1169](https://github.com/google/automotive-design-compose/issues/1169)
- **Author:** @rylin8
- **Created:** 2024-05-29T18:46:29Z
- **Updated:** 2024-05-29T18:46:29Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Variable support relies on variable IDs and names. These are unique within a single Figma file, but can conflict if using mulitple files. Consider adding support for duplicate variable names and IDs across multiple files.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1170"></a>Issue #1170: Scaling two separates instances of a component will always take the original size

- **URL:** [https://github.com/google/automotive-design-compose/issues/1170](https://github.com/google/automotive-design-compose/issues/1170)
- **Author:** @Heneveld
- **Created:** 2024-05-29T19:22:22Z
- **Updated:** 2024-06-07T17:47:57Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

I have a Component that is intended to be an icon that is either a red or green circle.
Now if I need to use that icon in two different nodes of which are different sizes I would expect to be able to scale that icon to the size allowed by its container. 

![image](https://github.com/google/automotive-design-compose/assets/138029735/194b9c9b-0118-43d1-8981-6e01d1247ae2)

There seems to be no way to adjust this even when telling the modifier to fill the max size.
[Ex4Google.zip](https://github.com/google/automotive-design-compose/files/15489867/Ex4Google.zip)


### Comments (1)

#### Comment #1 by @rylin8 on 2024-06-06T23:28:29Z

I believe the issue is that we don't yet support vector scaling properly: https://github.com/google/automotive-design-compose/issues/673.

Although this is not ideal, you could get around this by removing the Ellipse out of the RED and GREEN variants, and instead making those frames with large rounded corners.

![Screenshot 2024-06-06 at 4 27 25 PM](https://github.com/google/automotive-design-compose/assets/130500397/268e03b9-b452-41c4-8067-1ecd84c4169f)


---

## <a id="issue-1187"></a>Issue #1187: Fill with image not showing on the emulator

- **URL:** [https://github.com/google/automotive-design-compose/issues/1187](https://github.com/google/automotive-design-compose/issues/1187)
- **Author:** @MarcFerrerVWGS
- **Created:** 2024-06-03T06:25:44Z
- **Updated:** 2024-06-07T17:47:57Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Hi,

Currently on figma I have several elements that has a fill with an a export image from my computer. When I add that image and wait for the live update to show the image is never displayed on the emulator.

![image](https://github.com/google/automotive-design-compose/assets/156323011/f8d0f2f1-ea4e-4998-a744-dc32729f1c3b)

Is any solution or method to load the image from figma? The only workaround is to get the name on code and load the image to the component by code, like a placeholder but I would like not to do that with all the images.


### Comments (6)

#### Comment #1 by @rylin8 on 2024-06-03T16:12:53Z

Images in Figma should load on the emulator. Do you happen to have an image customization set for that node in code? If so, we assume you will replace the image from Figma with something, so we don't download it.


#### Comment #2 by @MarcFerrerVWGS on 2024-06-04T05:41:07Z

So currently I have my component with the fill property loaded with a custom image and then I am creating an instance of it. When I load on the emulator and wait for the live update the image never shows up, is empty and in code I have nothing refering to that component or node


#### Comment #3 by @rylin8 on 2024-06-04T16:59:27Z

That should definitely work, and we have tests with this scenario that work as well. I would suggest a couple of things:
- Remake the component in case there is a caching issue.
- Try a smaller image in case there is a size limit that we don't know about.

Are there any notable log messages that might be related? If none of this helps, please provide some example code along with a Figma document that we can use to look into the issue. Thanks!


#### Comment #4 by @MarcFerrerVWGS on 2024-06-05T05:33:34Z

In this zip there's the figma file with the image. I don't attach the code because I am doing nothing. Only loading the figma id document on the stage. I dont see any log or any error about that.
I can't see the image on the emulator.
[test_figma.zip](https://github.com/user-attachments/files/15573635/test_figma.zip)


#### Comment #5 by @rylin8 on 2024-06-05T15:59:39Z

Hi there, I tested this Figma design file and am able to load the image. Another suggestion is to delete the saved `.dcf` file on your emulator device with adb. Something like:
```
adb root
adb shell
find /data -name *YOUR_FIGMA_FILE_ID*
rm <PATH_TO_DCF_FILE>
```

Here is what my emulator looks like:
![Screenshot 2024-06-05 at 8 55 28 AM](https://github.com/google/automotive-design-compose/assets/130500397/ed7821e8-fd67-4332-9d21-aebc38fec2b2)


#### Comment #6 by @MarcFerrerVWGS on 2024-06-06T05:46:49Z

Hi, 
I tried again and the same issue. The image is not showing. I am working with an emulator with proxy applied, can be a problem to download imges?
This is the output I got:

I  Read 2349 bytes of doc vaMdKCW
I  Successfully deserialized V18 doc. Name: test_images, last modified: 2024-06-06T05:39:31Z
I  Decoded 1 images for test_images
I  Saving doc vaMdKCW to /data/user/10/seat.cupra.coachmarks/files/StageFrameDoc_vaMdKCWyY0hXaMkooGINlB.dcf
I  Save doc vaMdKCW success
I  Live update for vaMdKCW fetched and informed 1 subscribers


---

## <a id="issue-1295"></a>Issue #1295: Localization plugin UI polish

- **URL:** [https://github.com/google/automotive-design-compose/issues/1295](https://github.com/google/automotive-design-compose/issues/1295)
- **Author:** @yiqunw700
- **Created:** 2024-06-24T20:25:06Z
- **Updated:** 2024-07-15T18:53:59Z
- **Labels:** `plugin`, `i18n`
- **Assignees:** *None*

### Description

A follow up ticket of https://github.com/google/automotive-design-compose/issues/1242

Needs UX to review and provide design for the plugin UI polish.


### Comments (1)

#### Comment #1 by @yiqunw700 on 2024-07-15T18:52:59Z

https://github.com/google/automotive-design-compose/pull/1320#issuecomment-2226339021
```
"Submit" button is a little confusing as to whether it applies to the "Char Limit" and "Description", or just "Description".
```


---

## <a id="issue-1299"></a>Issue #1299: Control Figma UI prototype state in code

- **URL:** [https://github.com/google/automotive-design-compose/issues/1299](https://github.com/google/automotive-design-compose/issues/1299)
- **Author:** @miguel-galarza
- **Created:** 2024-06-27T09:36:29Z
- **Updated:** 2024-10-22T08:18:25Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

It is currently not possible to get information about the current UI prototype state in Figma from the code side in Android. For example, it is not possible to know the current screen being displayed which blocks some use cases such as:
- In a multiple-step-flow application containing several screens, it is not possible to manage the logic in each step properly due to a possible lost of synchronization between Figma and the code, and it can not be guaranteed which is the visible screen from code.
- Persist data of current screen for a later reload of the application.
- Implementation of a tabBar always visible to navigate through the application and highlight the current visible view including current state of the buttons.

It is neither possible to set a prototype state to be navigated to, which blocks the following use cases:
- Navigate to a predefined navigation when entering the application from two different entry points in the system, for example from the grid menu or from another application.
- Interaction with an external hardkey/device like steering wheel buttons or rotary knobs to control navigation of screens.


### Comments (1)

#### Comment #1 by @BIN-KOUHINN on 2024-10-22T08:18:24Z

+1
On feal project. get/set about the current UI prototype state in Figma from the code side in Android is necessary. 
e.g. Conditional screen transition from the code


---

## <a id="issue-1300"></a>Issue #1300: Drag-Drop implementation

- **URL:** [https://github.com/google/automotive-design-compose/issues/1300](https://github.com/google/automotive-design-compose/issues/1300)
- **Author:** @miguel-galarza
- **Created:** 2024-06-27T09:59:54Z
- **Updated:** 2024-09-10T06:19:55Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

It is needed the drag-drop behavior implementation where after a long press in an item it allows to move the element to a different position.


### Comments (2)

#### Comment #1 by @iamralpht on 2024-09-09T20:57:31Z

We don't have this currently -- for your implementation would it be within a single list, or between multiple lists in the same app, or between apps?


#### Comment #2 by @miguel-galarza on 2024-09-10T06:19:54Z

It would be between two lists located in the same screen.


---

## <a id="issue-1331"></a>Issue #1331: DesignVariant not working with figma component instances

- **URL:** [https://github.com/google/automotive-design-compose/issues/1331](https://github.com/google/automotive-design-compose/issues/1331)
- **Author:** @miguel-galarza
- **Created:** 2024-07-08T08:16:33Z
- **Updated:** 2025-10-31T13:06:31Z
- **Labels:** *None*
- **Assignees:** @rylin8

### Description

With the purpose of reusing Figma components, it is usually created a generic component in Figma with different variants. For example a #buttonGeneric with two variants (Enabled, Disabled) associated to a property #buttonState. From Figma, then different instances of the #buttonGeneric could be added in a screen and for each one it is possible to select a specific variant by modifying the property  #buttonState. The image below, describes the use case:

<img src="https://github.com/google/automotive-design-compose/assets/6141458/89a0421f-5cd4-45e3-ae3c-0af4492819ab" width="700">

The problem is that this approach of implementation is not working properly in design compose. When @DesignVariant is used, it is not possible to associate each specific instance to different buttons in the screen (nodes). Moreover when the variant is applied, the parameters of the node are lost:
- Tap on button stop working (TapCallback not triggered)
- TextButton associated references to the text of the #buttonGeneric instead of the associated to the node
- Icons associated references to the icon of the #buttonGeneric instead of the associated to the node
 
It seems that when using DesignVariant  a complete replacement of the instance by the generic one occurs.
 
The following issue [Cannot change variant of a component on the stage · Issue #691 · google/automotive-design-compose (github.com)](https://github.com/google/automotive-design-compose/issues/691) reports a similar perspective of the issue but this workaround add additional complexity to the code and does not solve the  TapCallback issue. The approach also loses the simplicity of adding simple nodes.

It would be really helpful to apply specific variants to specific nodes without the need of creating additional components for each case.


### Comments (11)

#### Comment #1 by @timothyfroehlich on 2024-07-11T14:06:44Z

Thank you for the detailed request. I've assigned this to @iamralpht so that we can triage this soon.


#### Comment #2 by @rylin8 on 2024-07-17T21:26:08Z

Are you trying to change the button variant at runtime? If not, you don't need to use `@DesignVariant`. Then you can name the instances differently and use those node names in your `@Design` annotations.


#### Comment #3 by @miguel-galarza on 2024-07-19T06:13:40Z

Hi @rylin8,
Yes, it is needed to change the button style at runtime, and there may be different states for different buttons in the same screen.


#### Comment #4 by @rylin8 on 2024-07-19T17:31:31Z

One thing you could do is create placeholder nodes in your main frame with different names like `#button1`, `#button2` etc. Then put `@Composable (ComponentReplacementContext) -> Unit` customizations on those nodes and replace them with a `@Composable` generated from a `@DesignComponent` function for that button. For an example see https://github.com/google/automotive-design-compose/blob/05569c2d087855e5be7b883be330bbe9c92144be/integration-tests/validation/src/main/java/com/android/designcompose/testapp/validation/examples/VariantPropertiesTest.kt#L101


#### Comment #5 by @miguel-galarza on 2024-07-23T10:04:22Z

Hi @rylin8,

I have tested the proposed solution by also adding a text and a TapCallback to the replacement composable. In the example provided in VariantPropertiesTest.kt, it would be something like this:

@DesignComponent(node = "#SquareBorder")
    fun Square(
        @DesignVariant(property = "BorderType") type: SquareBorder,
        @DesignVariant(property = "#SquareColor") color: SquareColor,
        @DesignVariant(property = "#SquareShadow") shadow: Shadow,
        @Design(node = "#SquareBorder") onTapCallback: TapCallback,
        @Design(node = "#text") textButton: String
    )

Independent buttons are now working as expected taking the states "Enabled" and "Disabled", but if we add some prototype change from Figma itself, for example a "While Pressing" then if two buttons have the same state both will be changed during the "While pressing".
![ButtonStates](https://github.com/user-attachments/assets/254e03b9-bda0-4fe8-927f-c963a3af35a2)

Is there some solution for this?

Thank you and BR,


#### Comment #6 by @rylin8 on 2024-07-23T16:52:22Z

I'm not sure why that wouldn't be working. We have a test that does something very similar to what you're describing. Please check this example and see if it is similar to your scenario:
https://github.com/google/automotive-design-compose/blob/main/integration-tests/validation/src/main/java/com/android/designcompose/testapp/validation/examples/VariantInteractionsTest.kt


#### Comment #7 by @iamralpht on 2024-09-09T20:55:43Z

@miguel-galarza Can you share your example (or a reduction of your example) that reproduces this issue, because we think we have a test case that covers it (but must have a variation that we're not covering)?


#### Comment #8 by @miguel-galarza on 2024-09-10T09:55:50Z

Hi,

I have repeated the tests with version "0.30.0-rc01". 

The use case would be to have some generic button that can used multiple times in a screen by setting individual text, and callback events. The button should contain the states: enabled, disabled and pressed while keeping the individual text associated, and callback to each button. I have included a screenshot of the figma document and a MainActivity.kt with the summary of the example. The generic button is called "#button1" and three instances of the button are added to the screen, two of them replaced with ComponentReplacementContext and the third not.

In the example the button states works individually, but the "whilePressing" event defined the Figma prototype does not work.

![Captura](https://github.com/user-attachments/assets/064430de-5c0e-4fb1-817f-ba8e21bdf99c)

MainActivity.kt


```
package com.example.designcomposenavtest

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.DesignSettings
import com.android.designcompose.TapCallback
import com.android.designcompose.annotation.Design
import com.android.designcompose.annotation.DesignComponent
import com.android.designcompose.annotation.DesignDoc
import com.android.designcompose.annotation.DesignVariant
import timber.log.Timber

enum class ButtonState {
    disabled,
    enabled
}

@DesignDoc(id = "id_document")
interface StageFrame {
    @DesignComponent(node = "#stage1", isRoot = true)
    fun Stage1(
        @Design(node = "#button1-stage1") button1: @Composable (ComponentReplacementContext) -> Unit,
        @Design(node = "#button2-stage1") button2: @Composable (ComponentReplacementContext) -> Unit,
    )

    @DesignComponent(node = "#button1")
    fun ButtonVariants(
        @DesignVariant(property = "#statebutton") buttonState: ButtonState,
        @Design(node = "#textButton") textButton: String,
        @Design(node = "#button1") onTapCallback: TapCallback
    )
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFigmaToken()
        DesignSettings.enableLiveUpdates(this)
        setContent {
            App()
        }
    }

    @Composable
    fun App() {

        //Button 1 States
        var buttonState1: ButtonState by remember { mutableStateOf(ButtonState.disabled) }
        var buttonText1: String by remember { mutableStateOf("Enabled b1") }

        //Button 2 States
        var buttonState2: ButtonState by remember { mutableStateOf(ButtonState.disabled) }
        var buttonText2: String by remember { mutableStateOf("Enabled b2") }

        StageFrameDoc.Stage1(
            button1 = {
                StageFrameDoc.ButtonVariants(
                    buttonState = buttonState1,
                    textButton = buttonText1,
                    onTapCallback = {
                        if (buttonState1 == ButtonState.enabled) {
                            buttonState1 = ButtonState.disabled
                            buttonText1 = "Disabled b1"
                        } else {
                            buttonState1 = ButtonState.enabled
                            buttonText1 = "Enabled b1"
                        }
                    }
                )
            },
            button2 = {
                StageFrameDoc.ButtonVariants(
                    buttonState = buttonState2,
                    textButton = buttonText2,
                    onTapCallback = {
                        if (buttonState2 == ButtonState.enabled) {
                            buttonState2 = ButtonState.disabled
                            buttonText2 = "Disabled b2"
                        } else {
                            buttonState2 = ButtonState.enabled
                            buttonText2 = "Enabled b2"
                        }
                    }
                )
            }
        )

        //Indicators of current status
        Text(
            modifier = Modifier.absoluteOffset(50.dp, 300.dp),
            text = "btn 1: $buttonText1",
            fontSize = 30.sp
        )
        Text(
            modifier = Modifier.absoluteOffset(50.dp, 400.dp),
            text = "btn 2: $buttonText2",
            fontSize = 30.sp
        )
    }

    private fun setFigmaToken() {
        val figmaAccessToken = "123"
        if (figmaAccessToken.isNotEmpty()) {
            val intent = Intent().apply {
                component =
                    ComponentName(
                        "com.example.designcomposenavtest",
                        "com.android.designcompose.ApiKeyService"
                    )
                action = "setApiKey"
                putExtra("ApiKey", figmaAccessToken)
            }
            startService(intent)
        } else {
            Timber.tag("MainActivity")
                .e("FIGMA_ACCESS_TOKEN is not defined. Skipping setting Figma token.")
        }
    }

}
```


#### Comment #9 by @rylin8 on 2025-02-10T16:15:15Z

Hi @miguel-galarza, I was just looking through old tickets and noticed that we hadn't responded to this, sorry about that. Is this still an issue for you? Perhaps you need to pass in a unique `key` parameter to `StageFrameDoc.ButtonVariants()` to achieve the correct `whilePressed` behavior.


#### Comment #10 by @StephanSchuster on 2025-10-29T20:35:11Z

We're also seeing the "Tap on button stop working (TapCallback not triggered)" issue in 0.37.2 as mentioned in the very first post.


#### Comment #11 by @StephanSchuster on 2025-10-31T13:06:31Z

> Hi [@miguel-galarza](https://github.com/miguel-galarza), I was just looking through old tickets and noticed that we hadn't responded to this, sorry about that. Is this still an issue for you? Perhaps you need to pass in a unique `key` parameter to `StageFrameDoc.ButtonVariants()` to achieve the correct `whilePressed` behavior.

Using "key" indeed fixes the "whilePressed" behavior. But I would rather see this as a workaround. If technically possible, I think this should work without "key".


---

## <a id="issue-1368"></a>Issue #1368: More flexibility possible?

- **URL:** [https://github.com/google/automotive-design-compose/issues/1368](https://github.com/google/automotive-design-compose/issues/1368)
- **Author:** @StephanSchuster
- **Created:** 2024-07-12T15:14:49Z
- **Updated:** 2024-11-28T08:34:31Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

I just started evaluating ADC. Really cool and useful. Thanks for all your work so far.

**Do you think the following two features would be possible in future releases of ADC in order to increase its flexibility:**
1. Load offline docs also from resources (not only assets) --> enables Runtime Resource Overlays (RRO)
2. Support dynamic `docId` and `rootNode` (e.g. via params) instead of generating/hardcoding them --> enables custom loading logic

Any feedback appreciated.

**More details on (1):**
As far as I can see, this is where you currently try to load offline `*.dcf` files from the `assets/figma` dir:
https://github.com/google/automotive-design-compose/blob/4dd69e283fcff3a0855c0bfdec2db951c972923c/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L513

_Before_ doing so, you could check in `res/raw/` and then fall back to `assets/figma`. In consequence we could apply new `*.dcf` files via RRO which would be a great and mighty new feature.


**More details on (2):**
Currently code like this
```
@DesignDoc(id = "XYZ...")
interface Test {

    @DesignComponent(node = "#MyRootNode")
    fun Main(
        ...
    )
}
```
results in the following generated code:
```
@Composable
final fun Main(...) {
    ...
    var nodeName = "#MyRootNode"
    ...
    val (docId, setDocId) = remember { mutableStateOf(DesignDocId("XYZ...", "")) }
    ....
}
```
See:
https://github.com/google/automotive-design-compose/blob/4dd69e283fcff3a0855c0bfdec2db951c972923c/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L642 https://github.com/google/automotive-design-compose/blob/4dd69e283fcff3a0855c0bfdec2db951c972923c/codegen/src/main/kotlin/com/android/designcompose/codegen/BuilderProcessor.kt#L779

I think if we could provide `docId` and `rootNode` dynamically, e.g. via optional params to the generated Composable, this would provide a hook for custom loading mechanisms. I other words, I would wish for a way to load a dedicated Figma doc/branch [like the DesignSwitcher](https://github.com/google/automotive-design-compose/blob/4dd69e283fcff3a0855c0bfdec2db951c972923c/designcompose/src/main/java/com/android/designcompose/DesignView.kt#L819) but from my own code.


### Comments (12)

#### Comment #1 by @StephanSchuster on 2024-07-15T10:28:13Z

Regarding (2): I guess I should simply not generate code via annotations but use the `DesignDoc` and `CustomizationContext` directly to achieve the requested dynamic/flexibility. In other words: write the generated code myself with the appropriate adaptions. Correct?


#### Comment #2 by @rylin8 on 2024-07-17T20:46:28Z

You can change the doc ID with the `DesignDocOverride` function. Example: https://github.com/google/automotive-design-compose/blob/c5af48beacc7afc0a7b936d70fe204ee15f49b2a/integration-tests/validation/src/main/java/com/android/designcompose/testapp/validation/examples/HelloBye.kt#L36

I'm not sure what your goal is exactly for changing the rootNode, but you can create different functions with different root nodes and call the appropriate one at runtime.


#### Comment #3 by @rylin8 on 2024-09-09T21:14:37Z

@StephanSchuster please let us know if the `DesignDocOverride()` function is sufficient.


#### Comment #4 by @StephanSchuster on 2024-11-14T10:11:47Z

Hi @rylin8, thanks for catching up and sorry that I did not reply since my initial question. I haven't had time since then to work with ADC. Now this hopefully changes.

You mentioned `DesignDocOverride()`. That's good to know, but not what I need. My overall goal is to have more control over the logic what to load from where and how to customize it - at runtime, dynamically, not via annotations. In my initial question I therefore asked for two related features.

I think for (2) I will be good to proceed as I mentioned in my second comment.

And for (1) I was hoping that you could add something like this into [DocServer](https://github.com/google/automotive-design-compose/blob/4dd69e283fcff3a0855c0bfdec2db951c972923c/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L515):

```kotlin
// ...
val assetManager = context.assets
try {
    @Suppress("DiscouragedApi")
    val resourceId = context.resources.getIdentifier("figma_${id.lowercase()}_dcf", "raw", context.packageName)
    val assetDoc = if (resourceId != 0) {
        // If file exists in res/raw, load from there
        context.resources.openRawResource(resourceId)
    } else {
        // Otherwise, load from assets (like done now)
        assetManager.open("figma/$id.dcf")
    }
    val decodedDoc = decodeDiskDoc(assetDoc, null, docId, Feedback)
    // ...
}
// ...
```
Background: files in `assets` cannot be overlayed via [Runtime Resource Overlays](https://source.android.com/docs/core/runtime/rros). Only files in `res` can. With this I could provide a new Figma DCF file bundeled in a RRO at runtime and the app would change its appearance.

Maybe the order should be the other way round: `assets` first, and only if not found, check `res`. With that, overlaying would be prevented if a file in assets is provided.

Do you think there is a chance that you will add such functionality?


#### Comment #5 by @timothyfroehlich on 2024-11-14T19:34:15Z

@StephanSchuster we actually did earlier this week! #1778. We haven't added documentation for it, but the test that was added should show how to use it. It will be released as part of 0.32, and we cut 0.32.0-rc01 the other day.


#### Comment #6 by @StephanSchuster on 2024-11-15T10:37:16Z

@timothyfroehlich, @dipenpradhan : Yes, yes, yes!!! This is awesome. Thank you so much. I think this is a great addition.

I just tested it in rc-01. Works! One thing, however ...

The current comment for `setRawResourceId(@RawRes resourceId: Int)` says "[Live updates do not work when this is set.](https://github.com/google/automotive-design-compose/blob/6960284018f09eb96523820cdefa21bbf87de0e6/designcompose/src/main/java/com/android/designcompose/DocServer.kt#L162)". From what I see in code and from what I saw in my tests, this is not true. Live updates DO work and have priority.

The priorities as far as I can tell are:

1. Live updates from Figma (if `DesignSettings.enableLiveUpdates(this) is set`)
2. Offline file from `res/raw` (if `DesignSettings.setRawResourceId(R.raw.xyz)` is set)
3. Offline file from `assets`
4. Downloaded/cached files from app's `filesDir`

For me, this totally makes sense. Can you confirm that the above is true and you will stick with this?


#### Comment #7 by @StephanSchuster on 2024-11-18T15:36:02Z

Along the idea of "more flexibility", would it be possible to have a `disableLiveUpdates()`?

Background: We want/need to toggle various `DesignSettings` at runtime or rather call them with different values. While this works for `setRawResourceId()` and more or less for `addFontFamily()` (a "clear" method would be nice), we currently can only `enableLiveUpdates()` but never switch it off again.


#### Comment #8 by @rylin8 on 2024-11-18T22:05:36Z

The comment regarding live update not working is a bit misleading. If you enable live update the design switcher will show errors and you likely won't be able to see your app. We are aware of this issue and filed https://github.com/google/automotive-design-compose/issues/1802 to fix it.


#### Comment #9 by @StephanSchuster on 2024-11-18T22:47:41Z

@rylin8 : I think the current logic/priorities/implementation is fine as it is (see 1.-4. above). It totally makes sense. Also the single res/raw. I did not experience any errors. I would not change anything. I would just fix the comment.


#### Comment #10 by @StephanSchuster on 2024-11-26T21:04:01Z

Thanks for releasing 0.32.0 including the new resource loading. This is an important first step for how we plan to use ADC in the future. And while I know that this is only the first shot, I have some more comments, questions and findings (which you might already be aware of).

Assume the following:
- My app contains a `res/raw/figma.dcf` file which can be overlayed via RRO
- My app loads/uses different nodes from Figma based on a `config` file which is applied and processed at runtime
- RRO 1 includes its own `res/raw/figma.dcf` file which contains only node `#A` plus a `config` file that results in using `#A` in code/UI
- RRO 2 includes its own `res/raw/figma.dcf` file which contains only node `#B` plus a `config` file that results in using `#B` in code/UI
- No files are currently cached in my app's `filesDir` nor does it have an *.dcf files in `assets`

Based on the above:
- RRO 1 is applied to my app --> `#A` is shown as configured and everything works as expected
- While the app is still running RRO 2 is applied (it does not matter if RRO 1 gets disabled first)
- Result: `#B` is not found because the old design doc with only `#A` was cached in memory and gets reused
- If at this point I would restart my app, memory gets cleared and I would see `#B` of the active RRO 2
- ADC caches docs based on docId which I think is good
- RRO 1 and RRO 2 use the same docId which I think should be valid/supported as well

Now that I am writing this, I am not 100% sure anymore if the same docId results from the dynamic way how we are using ADC or if this happens with static code as well. I could look it up, but I guess you know it by heart and can judge for yourself if this is a situation that you want to handle.

The way I am currently handling this is by clearing the ADC cache similar to your `ClearStateTestRule` whenever I notice that the MD5 hash of the `res/raw/figma.dcf` changed. In fact, I do this:
```kotlin
fun clearCache(context: Context, figmaConfig: FigmaConfig) {
    // Delete cache file to avoid outdated artifacts
    deleteCacheFile(context, figmaConfig)
    // Imitate "ClearStateTestRule" from ADC codebase
    clearStates()
    clearDocuments()
    clearFileFetchStatus()
    DesignSettings.setRawResourceId(Resources.ID_NULL)
    // Additional stuff that seems required or useful
    clearSubscriptions()
    clearFirstFetch()
}
```
Each method is currently using reflection. Yes, ugly, super ugly, but the only option I have as of now. Is there a chance that you offer a public API method to clear the cache in some way or the other? This might help others as well. Besides the scenario mentioned above, we also need to clear the ADC cache whenever we notice that because of a new configuration the list of needed Figma root nodes ("server params") changed. In that case we would also face the above "node not found" situation. Hence, we clear the cache and download a new doc with all needed root nodes.

Okay, this now gets a bit wired, sorry. **I guess in the end it boils down to: can we have a official `clearCache()` method?**

One last related thing: just skimmed again over the DocServer code. If I have setRawResource() and no live updates enabled (we also do this dynamically) but some cached files on `filesDir`, the latter take prescedence over the `raw/res/...dcf`. Correct? Is this a bug or a feature? Could in fact be both. If I am right, I am not sure which approach I would prefer. Just wanted to ask.


#### Comment #11 by @rylin8 on 2024-11-27T19:31:16Z

First, the `setRawResource` function now takes a Figma document ID as a parameter so that multiple documents can be set to load from different raw resources. This change will be available in 0.33.

Second, the load order is unfortunately a bit confusing at the moment. On app startup, it should load from assets/raw resources if it exists, at which point the doc is added to a memory cache. After that, the document will always be found in the cache. I realize this is not ideal for supporting RRO and changing the resource. This is a new feature that hasn't been full worked out yet. We will discuss this issue soon, I've filed https://github.com/google/automotive-design-compose/issues/1814 to track the issue. Thanks for the input!


#### Comment #12 by @StephanSchuster on 2024-11-28T08:34:29Z

@rylin8 : Thanks for your answer. We are looking forward to see this feature evolving. In case you want more input, also e.g. on our "dynamic usage" of ADC, we (@Elektrobit) would be happy to schedule a call.


---

## <a id="issue-1373"></a>Issue #1373: Create a better separation between toolkit_schema and figma_schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/1373](https://github.com/google/automotive-design-compose/issues/1373)
- **Author:** @timothyfroehlich
- **Created:** 2024-07-15T21:48:52Z
- **Updated:** 2024-08-21T20:58:33Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

I think we need to clean this up: we have a few places where `toolkit_schema` uses definitions from `figma_schema`, but the two should be unrelated, because we don't want changes to Figma's API to bleed into changes in our format... but similarly, changes to our format shouldn't break Figma deserialization.

So I think we probably want to have two `Rectangle`, `Vector`, and `Color` definitions and some glue to convert from Figma to Toolkit (or dc_bundle now), rather than having shared definitions regardless of who is sharing with whom...

_Originally posted by @iamralpht in https://github.com/google/automotive-design-compose/pull/1293#discussion_r1676321100_


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1419"></a>Issue #1419: This would be a candidate for writing an `Into` implementation in figma_schema

- **URL:** [https://github.com/google/automotive-design-compose/issues/1419](https://github.com/google/automotive-design-compose/issues/1419)
- **Author:** @timothyfroehlich
- **Created:** 2024-07-23T20:10:33Z
- **Updated:** 2024-07-23T20:10:33Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

This would be a candidate for writing an `Into` implementation in figma_schema

_Originally posted by @rylin8 in https://github.com/google/automotive-design-compose/pull/1414#discussion_r1688651237_


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1446"></a>Issue #1446: Animate between design token values

- **URL:** [https://github.com/google/automotive-design-compose/issues/1446](https://github.com/google/automotive-design-compose/issues/1446)
- **Author:** @rylin8
- **Created:** 2024-07-29T20:28:46Z
- **Updated:** 2024-09-25T22:13:00Z
- **Labels:** *None*
- **Assignees:** @rylin8

### Description

When using design tokens and the theme or mode changes, animate the transition.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1450"></a>Issue #1450: Begin tightly vetting Rust dependencies

- **URL:** [https://github.com/google/automotive-design-compose/issues/1450](https://github.com/google/automotive-design-compose/issues/1450)
- **Author:** @timothyfroehlich
- **Created:** 2024-07-29T21:51:28Z
- **Updated:** 2026-03-17T16:34:47Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Users of DesignCompose need to ensure that our dependencies are vetted. We'll need to put together a process for vetting dependencies and start working out what to do about the dependencies that we uses that aren't currently vetted.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1544"></a>Issue #1544: Improve error handling: propagate an error from Rust and show it in DesignSwitcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/1544](https://github.com/google/automotive-design-compose/issues/1544)
- **Author:** @yiqunw700
- **Created:** 2024-08-20T22:14:24Z
- **Updated:** 2024-08-20T22:14:24Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

We discussed this in our last meeting and decided to merge this change, but also make a ticket to improve error handling. We would like to be able to propagate an error from Rust, where it parses the Figma data, into the dcf file so that DC can show the error in the DesignSwitcher or in any other way that the app wishes. @yiqunw700 can you make a ticket for this?

_Originally posted by @rylin8 in https://github.com/google/automotive-design-compose/pull/1513#discussion_r1724026418_


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1555"></a>Issue #1555: More support for text stroke

- **URL:** [https://github.com/google/automotive-design-compose/issues/1555](https://github.com/google/automotive-design-compose/issues/1555)
- **Author:** @yiqunw700
- **Created:** 2024-08-22T20:15:46Z
- **Updated:** 2024-08-26T17:13:50Z
- **Labels:** `Type: New Feature`, `:blue_square:  P3`
- **Assignees:** @yiqunw700

### Description

#485 added basic support for text stroke
* centered
* solid

To be supported:
* Dash stroke
* Maybe hard to support, do some investigation on the outside/inside text stroke


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1568"></a>Issue #1568: Determine the plan for managing Rust dependencies

- **URL:** [https://github.com/google/automotive-design-compose/issues/1568](https://github.com/google/automotive-design-compose/issues/1568)
- **Author:** @timothyfroehlich
- **Created:** 2024-08-27T17:54:17Z
- **Updated:** 2026-03-17T16:34:26Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1690"></a>Issue #1690: Figma Collections, Modes, Variables not available for query from design compose.

- **URL:** [https://github.com/google/automotive-design-compose/issues/1690](https://github.com/google/automotive-design-compose/issues/1690)
- **Author:** @Heneveld
- **Created:** 2024-10-02T16:31:08Z
- **Updated:** 2024-11-18T21:58:41Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

There is currently no way to query what collections, modes or variables are available from the DCF so they can be listed or chosen by a user. Currently this is simply hardcoded to known values from the application side which doesn't allow Figma to add or remove existing keys without changes from Kotlin code.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-1721"></a>Issue #1721: Dropdown list,  Slider bar support

- **URL:** [https://github.com/google/automotive-design-compose/issues/1721](https://github.com/google/automotive-design-compose/issues/1721)
- **Author:** @BIN-KOUHINN
- **Created:** 2024-10-22T09:04:12Z
- **Updated:** 2024-11-18T21:58:25Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Hello guys,

**### About  dropdown list support**:
It is known that we can use Figma prototype to make a dropdown list.  **Want to know how  dropdown list supported by design compose?** Can we get/set the selection state of the  dropdown list of Figma prototype? Or how do we make a dropdown list working from code by design compose?
_dropdown list example:_
![Image](https://github.com/user-attachments/assets/32722fa7-a4aa-4a2f-b79c-bb2d8d651e0b)
Also see [Figma create dropdown list prototype.](https://www.youtube.com/watch?v=uI3k9Ol-Mp4)


**### About Slider bar support**
**How Slider bar is supported by design compose?** We need to get/set the sliderbar state from Android Code also, so only Figma prototype is not OK.
_Slider bar example:_
![Image](https://github.com/user-attachments/assets/3cb01fa5-6e26-4735-b1ce-712e4ca220f3)
Also see [Figma create sliderbar prototype.](https://www.youtube.com/watch?v=dmtR6mqWcf0)



Thanks in advance,
BIN


### Comments (1)

#### Comment #1 by @BIN-KOUHINN on 2024-10-23T09:01:20Z

Can anyone give some comments?  Thanks.


---

## <a id="issue-1792"></a>Issue #1792: Squoosh enabled cannot find variable for components imported from library

- **URL:** [https://github.com/google/automotive-design-compose/issues/1792](https://github.com/google/automotive-design-compose/issues/1792)
- **Author:** @Heneveld
- **Created:** 2024-11-14T20:02:33Z
- **Updated:** 2025-05-22T17:51:52Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Failed to get variable value for VariableID:23f1a32cb7a2218a7e74ae220898c966fe72e0d2/859:577 in doc o8DXx7m

Create a component in a figma library file.
Add Variables to that specific component.
Import that component into a new Figma File and drag onto root node.

Design Compose will not be able to find variables linked to that component.
However if the Component is added directly from Kotlin Design Compose generated interface then it will find the variable ids.


### Comments (1)

#### Comment #1 by @rylin8 on 2025-05-22T17:51:32Z

[Issue @2200](https://github.com/google/automotive-design-compose/issues/2200) should resolve the problem of the remote variables resulting in nothing being drawn, but this ticket can track the task of retrieving remote variables.


---

## <a id="issue-1798"></a>Issue #1798: Improvements for DesignSwitcher

- **URL:** [https://github.com/google/automotive-design-compose/issues/1798](https://github.com/google/automotive-design-compose/issues/1798)
- **Author:** @StephanSchuster
- **Created:** 2024-11-18T16:21:26Z
- **Updated:** 2024-11-18T21:57:32Z
- **Labels:** `Type: Customer Issue`
- **Assignees:** *None*

### Description

Maybe at some point the design switcher could get some more love. Here would be my wish list:

**1. Wrong offsets for "Show node names"**
If I render [edge-to-edge](https://developer.android.com/develop/ui/compose/layouts/insets) and compensate the system bars via `Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) { DesignSwitcher(modifier = Modifier.align(Alignment.TopEnd)) }` the DesignSwitcher is nicely placed top right in the content area but all "node names" seem to be off for the height of the status bar. I have not spent enough time to understand why, but I assume this has to do with the [absoluteOffset](https://github.com/google/automotive-design-compose/blob/37098594bb939a8269a3a1f7a4c6e507ed45c2da/designcompose/src/main/java/com/android/designcompose/DebugNodeManager.kt#L115). I would be really happy if you could have a look.

![Image](https://github.com/user-attachments/assets/8bb4e55c-d959-423b-b29e-0da81a0e5a71)


**2. Preconfigure options**
We would like to e.g. launch the DesignSwitcher with "Show mini messages" disabled for less distraction.


**3. Pixel details**
When switching between "Status" and "Options" there is a small pixel jump. Mini messages have a 1px border/space on top, the window does not. And maybe the overlay on "Change" could fill the whole switcher in order to reduce layers and visual complexity. I know that the DesignSwitcher it is a debug thingy but I assume these things are rather easy to fix and would add to a more professional look and feel and are worth the effort.

![Image](https://github.com/user-attachments/assets/39bdef7e-385a-4829-a3c8-6dbea86a2385)


**Last but no least a question:**
Is there currently (or in future) a way to customize the DesignSwitcher?


### Comments (1)

#### Comment #1 by @rylin8 on 2024-11-18T21:57:27Z

Thanks for the suggestions, we will take them into account. Sorry there is no way to customize the DesignSwitcher at the moment.


---

## <a id="issue-1814"></a>Issue #1814: Better support for RRO

- **URL:** [https://github.com/google/automotive-design-compose/issues/1814](https://github.com/google/automotive-design-compose/issues/1814)
- **Author:** @rylin8
- **Created:** 2024-11-27T19:29:47Z
- **Updated:** 2025-04-21T17:24:42Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

The `setRawResource()` API allows you to load a `.dcf` file from a raw resource instead of from assets. However, if this is done, and then the resource is dynamically changed using RRO, the original file is still cached in memory. Find a better solution to support RRO.


### Comments (2)

#### Comment #1 by @rylin8 on 2024-12-02T21:46:17Z

Note: when the RRO is changed dynamically, a configuration change is triggered which we can hook into to clear the cached value.


#### Comment #2 by @rylin8 on 2024-12-03T22:06:58Z

Notes for troubleshooting/changing RROs: https://source.android.com/docs/core/runtime/rro-troubleshoot


---

## <a id="issue-1994"></a>Issue #1994: Make component library with DC

- **URL:** [https://github.com/google/automotive-design-compose/issues/1994](https://github.com/google/automotive-design-compose/issues/1994)
- **Author:** @rylin8
- **Created:** 2025-01-14T00:03:29Z
- **Updated:** 2026-04-09T23:07:36Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Test creation of a component library using DesignCompose. Use some designs from https://www.figma.com/community/file/1035203688168086460/material-3-design-kit


### Comments (1)

#### Comment #1 by @anshu10sep on 2026-04-09T23:07:36Z

What is the requirement for this feature? Can you provide some additional details


---

## <a id="issue-2113"></a>Issue #2113: Component overrides next

- **URL:** [https://github.com/google/automotive-design-compose/issues/2113](https://github.com/google/automotive-design-compose/issues/2113)
- **Author:** @yiqunw700
- **Created:** 2025-03-11T22:19:36Z
- **Updated:** 2025-04-17T18:56:37Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

#2090 
- The override_table holds a name to override data map. If name is not identical, it will have unexpected behaviors.
- Only check if there is any character overrides, when text is overridden, use the text or styledText from the overrides_table stored in the component_info.
- Nested component instance override does not work.

Also:
- rounded corners (maybe also other shape data) is not calculated...


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2138"></a>Issue #2138: RB HVAC investigation

- **URL:** [https://github.com/google/automotive-design-compose/issues/2138](https://github.com/google/automotive-design-compose/issues/2138)
- **Author:** @yiqunw700
- **Created:** 2025-03-24T19:13:32Z
- **Updated:** 2025-04-02T18:25:25Z
- **Labels:** `SystemUI`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2145"></a>Issue #2145: RTL support

- **URL:** [https://github.com/google/automotive-design-compose/issues/2145](https://github.com/google/automotive-design-compose/issues/2145)
- **Author:** @yiqunw700
- **Created:** 2025-04-02T18:24:12Z
- **Updated:** 2025-04-02T18:25:14Z
- **Labels:** `SystemUI`
- **Assignees:** *None*

### Description

*No description provided.*


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2181"></a>Issue #2181: Customizations are not propagated to replacement component children

- **URL:** [https://github.com/google/automotive-design-compose/issues/2181](https://github.com/google/automotive-design-compose/issues/2181)
- **Author:** @rylin8
- **Created:** 2025-04-28T18:39:00Z
- **Updated:** 2025-04-28T18:39:11Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Customizations made on a root level function such as `Stage()` should propagate down to children, even if the child is a component replacement.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2183"></a>Issue #2183: Investigate improvements for testing for nodes

- **URL:** [https://github.com/google/automotive-design-compose/issues/2183](https://github.com/google/automotive-design-compose/issues/2183)
- **Author:** @rylin8
- **Created:** 2025-04-30T18:47:49Z
- **Updated:** 2025-04-30T18:47:49Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Since onNodeWithTag() only works for nodes that have click/tap interactions, it's hard to write tests that validate the existence of nodes. Investigate some ideas to improve this. Currently we add all text into a hash table for testing. Maybe we can do the same for nodes or variants. We want to make sure this does not affect performance.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2188"></a>Issue #2188: Add support for insets in the Scalable UI plugin

- **URL:** [https://github.com/google/automotive-design-compose/issues/2188](https://github.com/google/automotive-design-compose/issues/2188)
- **Author:** @rylin8
- **Created:** 2025-05-08T17:17:27Z
- **Updated:** 2025-05-08T17:22:29Z
- **Labels:** *None*
- **Assignees:** @farivar-google

### Description

This can be a couple of different ways. We can specify the insets in the plugin UI, or directly in Figma by defining some rules such as defining up to four optional rects in each variant named something like "inset-left", "inset-top", etc. If we choose the plugin UI route, this ticket can track the plugin changes. Otherwise, no code changes are needed in this repository. With both methods, the Scalable UI DCF parsing code needs to be updated to read the data.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2189"></a>Issue #2189: Add support for rounded corners in Scalable UI

- **URL:** [https://github.com/google/automotive-design-compose/issues/2189](https://github.com/google/automotive-design-compose/issues/2189)
- **Author:** @rylin8
- **Created:** 2025-05-08T17:22:04Z
- **Updated:** 2025-05-08T17:22:33Z
- **Labels:** *None*
- **Assignees:** @farivar-google

### Description

This likely does not need any code changes and is a ticket to track the exploration of this feature. To achieve rounded corners, we can probably just specify the rounded corners on the "main" node of each variant, and the Scalable UI DCF parsing code needs to be updated to read the rounded corner data.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2190"></a>Issue #2190: Allow progress bar and indicator preview simultaneously

- **URL:** [https://github.com/google/automotive-design-compose/issues/2190](https://github.com/google/automotive-design-compose/issues/2190)
- **Author:** @rylin8
- **Created:** 2025-05-08T18:45:50Z
- **Updated:** 2025-05-08T18:46:07Z
- **Labels:** *None*
- **Assignees:** @farivar-google

### Description

The Dials & Gauges plugin allows you to set a node as a progress bar or progress indicator. However, there are many use cases where you want both to function together. To do this, you need to enable them individually, and the preview functionality using the slider only previews one of the two nodes.

Find a way to allow one slider to control both nodes.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2208"></a>Issue #2208: font family variables support

- **URL:** [https://github.com/google/automotive-design-compose/issues/2208](https://github.com/google/automotive-design-compose/issues/2208)
- **Author:** @farivar-google
- **Created:** 2025-05-20T18:20:55Z
- **Updated:** 2025-05-20T18:21:31Z
- **Labels:** `Type: New Feature`
- **Assignees:** *None*

### Description

change the font family based on certain situations.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2245"></a>Issue #2245: Scope Figma Draw features

- **URL:** [https://github.com/google/automotive-design-compose/issues/2245](https://github.com/google/automotive-design-compose/issues/2245)
- **Author:** @iamralpht
- **Created:** 2025-07-01T11:51:07Z
- **Updated:** 2025-07-05T05:30:09Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Amongst others, [Figma Draw](https://help.figma.com/hc/en-us/articles/31440394517143-Explore-Figma-Draw) adds support for text on a path, which would be very helpful for instrument cluster designs.


### Comments (1)

#### Comment #1 by @iamralpht on 2025-07-05T05:30:09Z

[I had a go at text path support](https://github.com/google/automotive-design-compose/commit/62da8cb2da4c29af40ca49289f18e7d546827d54), but unfortunately got blocked by issues with Figma's API implementation (meaning that it's not possible to do an end-to-end implementation yet).


---

## <a id="issue-2261"></a>Issue #2261: Same Figma node names do not execute smart animations.

- **URL:** [https://github.com/google/automotive-design-compose/issues/2261](https://github.com/google/automotive-design-compose/issues/2261)
- **Author:** @Gurupreet
- **Created:** 2025-07-21T13:15:34Z
- **Updated:** 2025-07-21T13:15:37Z
- **Labels:** *None*
- **Assignees:** @Gurupreet

### Description

When the name of two nodes are same, Figma is executing smart animation between then but DesignCompose doesn't perform any animation instead visibility is changed instantly.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2330"></a>Issue #2330: Layers with opacity start flickering when placed on another layer with opacity

- **URL:** [https://github.com/google/automotive-design-compose/issues/2330](https://github.com/google/automotive-design-compose/issues/2330)
- **Author:** @Elektrobit-HendrikCammann
- **Created:** 2025-11-13T18:17:52Z
- **Updated:** 2025-11-13T18:17:52Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

**Setup**

We have a frame with a solid color (e.g., #000) (happens more often when its a Variable), and we apply layer opacity (not color opacity) of, for example, 80%.
Then we place this frame on top of another frame which is an image or an frame with a gradient which has layer opacity applied

**Issue Description**

In rare cases the Layer starts flickering
https://github.com/user-attachments/assets/382cb768-a1e1-4163-b17b-abf8314e82e4

**Expected Behavior**

The color value is not changing

Version: 0.38.0


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2332"></a>Issue #2332: Component Instances are not render correctly until detached

- **URL:** [https://github.com/google/automotive-design-compose/issues/2332](https://github.com/google/automotive-design-compose/issues/2332)
- **Author:** @Elektrobit-HendrikCammann
- **Created:** 2025-11-13T18:29:53Z
- **Updated:** 2026-04-09T22:59:36Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

**Setup**

Create a component and place several instances of it on a screen.

**Issue Description**

In rare cases, one of the instances renders incorrectly (most commonly with mispositioned items), while the other instances render correctly. As soon as the “broken” instance is detached, it renders correctly again.

Version: 0.38.0


### Comments (1)

#### Comment #1 by @anshu10sep on 2026-04-09T22:59:36Z

Can you please provide additional information and maybe a .fig file or a test app to repro this with step-by-step instructions to validate it


---

## <a id="issue-2401"></a>Issue #2401: Support Figma OAuth 2.0 Token Authentication in Addition to Personal Access Tokens

- **URL:** [https://github.com/google/automotive-design-compose/issues/2401](https://github.com/google/automotive-design-compose/issues/2401)
- **Author:** @ChristianDittrich
- **Created:** 2026-03-16T13:51:38Z
- **Updated:** 2026-03-16T13:51:38Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

Currently, the integration with Figma only supports Personal Access Tokens for authentication. This request proposes adding support for Figma OAuth 2.0 tokens as an alternative authentication method.
https://developers.figma.com/docs/rest-api/authentication/

Figma Personal Access Tokens, while straightforward to use, come with several limitations in team and production environments:
- Security risk: PATs are long-lived credentials tied directly to a user's account. If leaked, they grant broad access with no easy scope limitation.
- User-bound: PATs are coupled to an individual's Figma account, making them fragile in team workflows -  if that person leaves, tokens break.
- Figma's own recommendation: Figma encourages OAuth 2.0 for integrations that act on behalf of users.

Proposed Solution:
Extend the authentication layer to accept OAuth 2.0 access tokens alongside the existing PAT support. This would involve:
Allowing the user to configure an OAuth 2.0 access token (and optionally a refresh token) in place of a PAT.
Switching the HTTP client from the PAT-style header (X-Figma-Token: <token>) to the OAuth 2.0 style header (Authorization: Bearer <token>) when an OAuth token is configured.


### Comments (0)

*No comments on this issue.*

---

## <a id="issue-2456"></a>Issue #2456: Cannot apply Android Modifier-based animations to child components; works only via ComponentReplacementContext

- **URL:** [https://github.com/google/automotive-design-compose/issues/2456](https://github.com/google/automotive-design-compose/issues/2456)
- **Author:** @kingartjr
- **Created:** 2026-05-05T22:22:04Z
- **Updated:** 2026-05-19T15:22:23Z
- **Labels:** *None*
- **Assignees:** *None*

### Description

## Description

It is well understood that **Figma animations do not transfer to Design Compose**.  
In this case, all animations are **implemented on the Android side using Compose `Modifier`s**, not relying on any Figma animation behavior.

I am able to apply and drive these animations when attaching them at the **screen level** (`#Home`), but I cannot apply them at the **child tile level** because accessing a tile’s `Modifier` does not behave as expected. As a result, I am forced to use `ComponentReplacementContext` for each tile.

***

## Figma Structure (Simplified)

    #Home (screen)
     ├─ #MusicTile
     ├─ #PhoneTile
     └─ #NavTile

Each tile:

*   Has a **front** and **back**
*   Uses a **flip animation implemented in Android** (e.g., `graphicsLayer`, `animateFloatAsState`, etc.)
*   Does **not** rely on Figma-defined animations

***

## Expected Behavior

Each tile should expose its `Modifier` so Android-side Compose animations can be applied directly to that tile (e.g., flip, rotation, scale).

This would allow animations like:

*   Flipping individual tiles
*   Driving animation state per tile
*   Avoiding full component replacement

***

## Actual Behavior

*   Attaching a `Modifier` at `#Home` works
*   Attempting to attach a `Modifier` directly to `#MusicTile`, `#PhoneTile`, or `#NavTile` does **not**
*   Android-side animations never run when using tile-level `Modifier` access
*   The only working solution is to use `ComponentReplacementContext`

***

## Reproduction

### ❌ Failing Approach — Modifier Access on Child Components

```kotlin
@DesignComponent(node = "#Home")
fun HomeWithTileModifiers(
    @Design(node = "#MusicTile") musicTileModifier: Modifier,
    @Design(node = "#PhoneTile") phoneTileModifier: Modifier,
    @Design(node = "#NavTile") navTileModifier: Modifier,
)
```

*   Modifiers are received
*   UI renders
*   Android-side animations applied via modifiers do **not** take effect

***

### ✅ Working Approach — Component Replacement

```kotlin
@DesignComponent(node = "#Home")
fun HomeWithReplacement(
    @Design(node = "#MusicTile")
    tile1: @Composable (ComponentReplacementContext) -> Unit,

    @Design(node = "#PhoneTile")
    tile2: @Composable (ComponentReplacementContext) -> Unit,

    @Design(node = "#NavTile")
    tile3: @Composable (ComponentReplacementContext) -> Unit,
)
```

*   Android-side animations work correctly
*   Each tile can independently animate (flip, rotate, etc.)
*   No reliance on Figma animations

***

## Question / Clarification

Is accessing a child component’s `Modifier` intended to support Android-side Compose animations?

If not:

*   Is this a known design limitation?
*   Should documentation recommend `ComponentReplacementContext` for per-component animation?

If yes:

*   Is this behavior a bug?
*   Are there constraints on what nodes can safely expose Modifiers for animation?

***

## Why This Matters

Relying exclusively on `ComponentReplacementContext`:

*   Adds extra boilerplate
*   Reduces composability
*   Makes simple per-component animations heavier than necessary

Direct `Modifier` access would better align with standard Compose patterns for fine-grained animation control.


### Comments (1)

#### Comment #1 by @hbowden on 2026-05-19T15:22:23Z

I was experiencing the same issue and made a branch that fixed the issue for me: https://github.com/hbowden/automotive-design-compose/tree/nested-component-modifier-fix

 **Root Cause:**
  Modifiers registered via @Design(node = "#NodeName") modifier: Modifier
  weren't being applied to nested components because Squoosh renders child
  visuals in the parent's draw block. The modifier had no Compose Layout node to
   apply against.
  
  **The Fix:**
  When a nested component has a registered modifier (and isn't already handled
  by replacement/scroll/list), the fix creates a sub-SquooshRoot with
  renderSubtree = true. This wraps the rendered pixels with the user's Modifier,
   allowing graphicsLayer, animateFloatAsState, etc. to work correctly.
  
  **Verification:**
  Tested with animated flip transformation on nested tile components:
  @Design(node = "#homeTileMap") modifierMapTile: Modifier
  Applied with:
  modifierMapTile = Modifier
      .clickable { flipped = !flipped }
      .graphicsLayer {
          rotationY = animatedRotation
          cameraDistance = 12f * density
      }   
      
  Animations now work on nested components without requiring
  ComponentReplacementContext.


---

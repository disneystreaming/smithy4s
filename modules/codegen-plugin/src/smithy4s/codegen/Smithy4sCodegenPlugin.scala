/*
 *  Copyright 2021-2024 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.codegen

import sbt.Keys._
import sbt.util.CacheImplicits._
import sbt.{fileJsonFormatter => _, _}
import scala.util.{Success, Try}
import JsonConverters._

object Smithy4sCodegenPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  object autoImport {
    val AWS = smithy4s.codegen.AwsSpecs

    val smithy4sCodegen =
      taskKey[Seq[File]](
        "Generate .scala and other files from smithy specs (.smithy or .json files)"
      )

    val smithy4sVersion =
      settingKey[String]("Smithy4sVersion")

    val smithy4sInputDirs = settingKey[Seq[File]](
      "Input directories for smithy specs (.smithy or .json files)"
    )

    val smithy4sOutputDir =
      settingKey[File](
        "Output directory for .scala files generated by smithy4s"
      )

    val smithy4sResourceDir =
      settingKey[File](
        "Output directory for non-Scala files generated by smithy4s"
      )

    val smithy4sGeneratedSmithyMetadataFile =
      settingKey[File](
        "Path of the the smithy metadata file generated by smithy4s"
      )

    val smithy4sAllowedNamespaces =
      settingKey[List[String]](
        "Allow-list of namespaces that should be processed by the generator. If unset, considers all namespaces but stdlib ones"
      )

    val smithy4sExcludedNamespaces =
      settingKey[List[String]](
        "Disallow-list of namespaces that should not be processed by the generator. When set, namespaces are evicted as the last filtering step"
      )

    val smithy4sSmithyLibrary =
      settingKey[Boolean](
        "Sets whether this project should be used as a Smithy library by packaging the Smithy specs in the resulting jar"
      )

    val smithy4sExplicitCodegenOnlyDependencies =
      taskKey[Seq[ModuleID]](
        List(
          "List of explicitly defined libraries for external dependencies that should be added to the classpath used by Smithy4s during code-generation",
          "The smithy files and smithy validators contained by these jars are included in the Smithy4s code-generation process",
          "Namespaces that were used for code generation in these dependencies will be excluded from code generation in this project.",
          "By default, this includes the library dependencies annotated with the `Smithy4s` configuration of the current project and its upstreams"
        ).mkString(" ")
      )

    val smithy4sExternallyTrackedDependencies =
      taskKey[Seq[ModuleID]](
        List(
          "List of libraries that external dependencies indicate having used during their own code-generation process",
          "If a project using Smithy4s depends on a library that contains Smithy4s generated code, local Smithy files might need the jars",
          "that were used by Smithy4s when the library in question was built. By default, Smithy4s adds a line in the jar manifests of the",
          "projects it is enabled on, to inform downstream projects of the jars they might want to pull during their own code-generation.",
          "This is different from a transitive compile dependency, as the jars used during code-generation might not necessarily end up",
          "on the compile class-path of a project"
        ).mkString(" ")
      )

    val smithy4sNormalExternalDependencies =
      taskKey[Seq[ModuleID]](
        List(
          "List of libraries that are declared normally and that will be added to the classpath of the code-generation process",
          "By default, this includes all `libraryDependencies` matching the scope, both local and transitive"
        ).mkString(" ")
      )

    val smithy4sAllExternalDependencies =
      taskKey[Seq[ModuleID]](
        List(
          "Exhaustive list of external dependencies that should be added to the classpath used by Smithy4s during code-generation",
          "The smithy files and smithy validators contained by these jars are included in the Smithy4s code-generation process",
          "Namespaces that were used for code generation in these dependencies will be excluded from code generation in this project."
        ).mkString(" ")
      )

    val smithy4sInternalDependenciesAsJars =
      taskKey[Seq[File]](
        List(
          "List of jars of internal dependencies that should be added to the classpath used by Smithy4s during code-generation",
          "The smithy files and smithy validators contained by these jars are included in the Smithy4s code-generation process",
          "Namespaces that were used for code generation in these dependencies will be excluded from code generation in this project.",
          "By default, this includes the jars produced by packaging this project's local dependencies, which implies these should compile for the codegen task to run",
          "This can be set to an empty list to prevent the inclusion of local dependencies during the code-gen process"
        ).mkString(" ")
      )

    val smithy4sAllDependenciesAsJars =
      taskKey[Seq[File]](
        List(
          "List of all jars for internal and external dependencies that should be used as sources of Smithy specs.",
          "Namespaces that were used for code generation in these upstream dependencies will be excluded from code generation in this project."
        ).mkString(" ")
      )

    val smithy4sWildcardArgument =
      taskKey[String](
        "String value to use as wildcard argument in types in generated code"
      )

    val smithy4sRenderOptics =
      taskKey[Boolean](
        "Boolean value to indicate whether or not to generate optics"
      )

    val smithy4sGeneratedSmithyFiles =
      taskKey[Seq[File]](
        "Generated smithy files"
      )

    val Smithy4s =
      config("smithy4s").describedAs(
        "Dependencies containing Smithy code, used at codegen-time only."
      )

    val smithy4sModelTransformers =
      settingKey[List[String]](
        "List of transformer names that should be applied to the model prior to codegen"
      )

    val smithy4sAwsSpecsVersion =
      settingKey[String](
        "Known version of the AWS specifications, produced by https://github.com/disneystreaming/aws-sdk-smithy-specs"
      )

    val smithy4sAwsSpecs =
      settingKey[Seq[String]]("Aws modules to load")

    val smithy4sAwsSpecDependencies =
      taskKey[Seq[ModuleID]](
        List(
          "List of modules containing AWS specifications that should be added to the classpath used by Smithy4s during code-generation"
        ).mkString(" ")
      )
  }

  import autoImport._

  override lazy val buildSettings = Seq(
    smithy4sVersion := BuildInfo.version
  )

  override def projectConfigurations: Seq[Configuration] = Seq(Smithy4s)

  // Use this with any configuration to enable the codegen in it.
  def defaultSettings(config: Configuration) = Seq(
    config / smithy4sInputDirs := Seq(
      (config / sourceDirectory).value / "smithy",
      (config / sourceManaged).value / "smithy"
    ),
    config / unmanagedSourceDirectories ++= (config / smithy4sInputDirs).value,
    config / smithy4sOutputDir := (config / sourceManaged).value / "scala",
    config / smithy4sResourceDir := (config / resourceManaged).value,
    config / smithy4sCodegen := cachedSmithyCodegen(config).value,
    config / smithy4sSmithyLibrary := true,
    smithy4sAwsSpecs := Seq.empty,
    smithy4sAwsSpecsVersion := smithy4s.codegen.AwsSpecs.knownVersion,
    Compile / smithy4sAwsSpecDependencies := {
      val version = (smithy4sAwsSpecsVersion).value
      (smithy4sAwsSpecs).value.map { case artifactName =>
        smithy4s.codegen.AwsSpecs.org % artifactName % version
      }
    },
    config / smithy4sInternalDependenciesAsJars := {
      (config / internalDependencyAsJars).value.map(_.data)
    },
    config / smithy4sExplicitCodegenOnlyDependencies := {
      transitiveLibraryDependencies.value
        .filter(
          _.configurations.exists(_.contains(Smithy4s.name))
        )
        .map(_.withConfigurations(None))
        .distinct
    },
    config / smithy4sExternallyTrackedDependencies := {
      (config / externalDependencyClasspath).value
        .map(_.data)
        .filter(_.ext == "jar")
        .flatMap(extractJar)
        .distinct
    },
    config / smithy4sNormalExternalDependencies := {
      (config / externalDependencyClasspath).value
        .flatMap(_.metadata.get(moduleID.key))
        .distinct
    },
    config / smithy4sAllExternalDependencies := {
      val all = (config / smithy4sNormalExternalDependencies).value ++
        (config / smithy4sExplicitCodegenOnlyDependencies).value ++
        (config / smithy4sExternallyTrackedDependencies).value ++
        (config / smithy4sAwsSpecDependencies).value
      all.distinct
    },
    config / smithy4sAllDependenciesAsJars := {
      (config / smithy4sInternalDependenciesAsJars).value ++
        fetch(config / smithy4sAllExternalDependencies).value
    },
    config / smithy4sWildcardArgument := {
      // This logic configures the default wildcard argument based on the scala version and scalac options
      // In the following scenarios we use "?" instead of "_"
      // 1. Scala version >= 3.1 ("_" is deprecated in 3.1 and becomes an error in 3.2)
      // 2. Scala version is 3 and "-source:future" or "-source future" are in scalac options
      val version = (config / scalaVersion).value
      val majorVersion = version.takeWhile(_ != '.')
      val minorVersion =
        version.drop(majorVersion.length + 1).takeWhile(_ != '.')
      def scalaOptionsContainsSourceFuture() = {
        val options = (config / scalacOptions).value
        options.contains("-source:future") || options
          .sliding(2, 1)
          .contains(Seq("-source", "future"))
      }
      (Try(majorVersion.toInt), Try(minorVersion.toInt)) match {
        case (Success(3), Success(minorVersion)) if minorVersion >= 1 => "?"
        case (Success(3), _) if scalaOptionsContainsSourceFuture()    => "?"
        case _                                                        => "_"
      }
    },
    config / smithy4sRenderOptics := false,
    config / smithy4sGeneratedSmithyMetadataFile := {
      (config / sourceManaged).value / "smithy" / "generated-metadata.smithy"
    },
    config / smithy4sGeneratedSmithyFiles := {
      val cacheFactory = (config / streams).value.cacheStoreFactory
      val cached = Tracked.inputChanged[(String, Boolean), Seq[File]](
        cacheFactory.make("smithy4sGeneratedSmithyFilesInput")
      ) { case (changed, (wildcardArg, shouldGenerateOptics)) =>
        val lastOutput = Tracked.lastOutput[Boolean, Seq[File]](
          cacheFactory.make("smithy4sGeneratedSmithyFilesOutput")
        ) { case (changed, prevResult) =>
          if (changed || prevResult.isEmpty) {
            val file = (config / smithy4sGeneratedSmithyMetadataFile).value
            IO.write(
              file,
              s"""$$version: "2"
                 |metadata smithy4sWildcardArgument = "$wildcardArg"
                 |metadata smithy4sRenderOptics = $shouldGenerateOptics
                 |""".stripMargin
            )
            Seq(file)
          } else {
            prevResult.get
          }
        }
        lastOutput(changed)
      }
      val wildcardArg = (config / smithy4sWildcardArgument).value
      val generateOptics = (config / smithy4sRenderOptics).value
      cached((wildcardArg, generateOptics))
    },
    config / sourceGenerators += (config / smithy4sCodegen).map(
      _.filter(_.ext == "scala")
    ),
    config / resourceGenerators += (config / smithy4sCodegen).map(
      _.filter(_.ext != "scala")
    ),
    config / cleanFiles += (config / smithy4sOutputDir).value,
    config / cleanFiles += (config / smithy4sResourceDir).value,
    config / smithy4sModelTransformers := List.empty,
    config / packageBin / packageOptions += {
      // This piece of logic aims at tracking the dependencies that Smithy4s used to generate
      // code at build time, in the manifest of the jar. This helps automatically pulling
      // the corresponding jars and prevents the users from having to search
      import java.util.jar.Manifest
      val manifest = new Manifest
      val scalaBin = scalaBinaryVersion.?.value
      val deps = libraryDependencies.value
        .filter(_.configurations.exists(_.contains(Smithy4s.name)))
        .flatMap(moduleIdEncode(_, scalaBin))
        .mkString(",")

      manifest
        .getMainAttributes()
        .put(new java.util.jar.Attributes.Name(SMITHY4S_DEPENDENCIES), deps)
      Package.JarManifest(manifest)
    }
  )

  override lazy val projectSettings =
    defaultSettings(Compile) ++ Seq(
      libraryDependencies ++= Seq(
        BuildInfo.alloyOrg % "alloy-core" % BuildInfo.alloyVersion % Smithy4s
      )
    )

  override lazy val globalSettings: Seq[Def.Setting[_]] = List(
    commands += GenerateSmithyBuild.command
  )

  private[codegen] def moduleIdEncode(
      moduleId: ModuleID,
      scalaBinaryVersion: Option[String]
  ): List[String] = {
    (moduleId.crossVersion, scalaBinaryVersion) match {
      case (Disabled, _) =>
        List(s"${moduleId.organization}:${moduleId.name}:${moduleId.revision}")
      case (_: Binary, Some(sbv)) =>
        List(
          s"${moduleId.organization}:${moduleId.name}_${sbv}:${moduleId.revision}"
        )
      case (_, _) => Nil
    }
  }

  /**
   * Retrieves the smithy4sDependencies that compile-dependencies may have listed
   * in their jar manifests when they were packaged.
   */
  private def fetch(
      dependenciesTask: Def.Initialize[Task[Seq[ModuleID]]]
  ): Def.Initialize[Task[Seq[File]]] =
    Def.task {

      def getJars(ids: Seq[ModuleID]): Seq[File] = {
        val syntheticModule =
          organization.value % (name.value + "-smithy4s-resolution") % version.value
        val depRes = (update / dependencyResolution).value
        val updc = (update / updateConfiguration).value
        val uwconfig = (update / unresolvedWarningConfiguration).value
        val smInfo = (update / scalaModuleInfo).value
        val modDescr = depRes.moduleDescriptor(
          syntheticModule,
          ids.toVector,
          smInfo
        )

        depRes
          .update(
            modDescr,
            updc,
            uwconfig,
            streams.value.log
          )
          .map(_.allFiles)
          .fold(uw => throw uw.resolveException, identity)
      }
      // Forcing configurations to None as the dynamic fetcher seems to omit
      // every moduleID that has a configuration.
      getJars(dependenciesTask.value.map(_.withConfigurations(None)))
    }

  def transitiveLibraryDependencies: Def.Initialize[Task[Seq[ModuleID]]] = {
    val make = new ScopeFilter.Make {}
    import make.{inDependencies => inDeps, _}
    val selectDeps = ScopeFilter(inDeps(ThisProject, includeRoot = true))
    val allDeps = libraryDependencies.?.all(selectDeps)
    Def
      .taskDyn(
        allDeps.map(_.flatMap(_.getOrElse(Seq.empty)))
      )
  }

  private lazy val simple = raw"([^:]*):([^:]*):([^:]*)".r
  private lazy val cross = raw"([^:]*)::([^:]*):([^:]*)".r
  private def extractJar(jarFile: java.io.File): Seq[ModuleID] = {
    JarUtils
      .extractSmithy4sDependencies(jarFile)
      .collect {
        case cross(org, art, version)  => org %% art % version
        case simple(org, art, version) => org % art % version
      }
  }

  def cachedSmithyCodegen(conf: Configuration) = Def.task {
    val inputDirs = Option((conf / smithy4sInputDirs).value).getOrElse(Seq())
    val generatedFiles = (conf / smithy4sGeneratedSmithyFiles).value
    val inputFiles =
      (inputDirs ++ generatedFiles)
        .filter(_.exists())
        .toList
    val outputPath = (conf / smithy4sOutputDir).value
    val resourceOutputPath = (conf / smithy4sResourceDir).value
    val allowedNamespaces =
      (conf / smithy4sAllowedNamespaces).?.value.map(_.toSet)
    val excludedNamespaces =
      (conf / smithy4sExcludedNamespaces).?.value.map(_.toSet)
    val localJars =
      (conf / smithy4sAllDependenciesAsJars).value.map(os.Path(_)).toList
    val res =
      (conf / resolvers).value.toList.collect { case m: MavenRepository =>
        m.root
      }
    val transforms = (conf / smithy4sModelTransformers).value
    val s = (conf / streams).value
    val skipResources: Set[FileType] =
      if ((conf / smithy4sSmithyLibrary).value) Set.empty
      else Set(FileType.Resource)
    val skipSet = skipResources

    val filePaths = inputFiles.map(_.getAbsolutePath())
    val codegenArgs = CodegenArgs(
      filePaths.map(os.Path(_)).toList,
      output = os.Path(outputPath),
      resourceOutput = os.Path(resourceOutputPath),
      skip = skipSet,
      discoverModels = false,
      allowedNS = allowedNamespaces,
      excludedNS = excludedNamespaces,
      repositories = res,
      dependencies = List.empty,
      transformers = transforms,
      localJars = localJars
    )

    val cached =
      Tracked.inputChanged[CodegenArgs, Seq[File]](
        s.cacheStoreFactory.make("input")
      ) {
        Function.untupled {
          Tracked.lastOutput[(Boolean, CodegenArgs), Seq[File]](
            s.cacheStoreFactory.make("output")
          ) { case ((inputChanged, args), outputs) =>
            if (inputChanged || outputs.isEmpty) {
              val resPaths = smithy4s.codegen.Codegen
                .generateToDisk(args)
                .toList
              resPaths.map(path => new File(path.toString))
            } else {
              outputs.getOrElse(Seq.empty)
            }
          }
        }
      }

    cached(codegenArgs)
  }
}

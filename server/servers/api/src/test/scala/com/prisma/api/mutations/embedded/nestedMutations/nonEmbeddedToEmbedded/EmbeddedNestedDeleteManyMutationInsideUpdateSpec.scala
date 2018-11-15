package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBase
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedTypesCapability, JoinRelationLinksCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedDeleteManyMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "A 1-n relation" should "error if trying to use nestedDeleteMany" in {
    val project = SchemaDsl.fromString() { embeddedP1opt }
    database.setup(project)

    val parent1Id = server
      .query(
        """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
        project
      )
      .pathAsString("data.createParent.id")

    val res = server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {deleteMany: {
         |        where:{c: "c"}
         |    }}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 0,
      errorContains = """ Reason: 'childOpt.deleteMany' Field 'deleteMany' is not defined in the input type 'ChildUpdateOneInput'."""
    )
  }

  "a PM to CM  relation " should "work" in {
    val project = SchemaDsl.fromString() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: {
         |          c_contains:"c1"
         |      }
         |    }
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "work with several deleteManys" in {
    val project = SchemaDsl.fromString() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"1"
         |    },
         |    {
         |        c_contains:"3"
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "work with empty Filter" in {
    val project = SchemaDsl.fromString() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {}
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  "a PM to CM  relation " should "not change anything when there is no hit" in {
    val project = SchemaDsl.fromString() { embeddedPM }
    database.setup(project)

    setupData(project)

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {deleteMany: [
         |    {
         |        c_contains:"3"
         |    },
         |    {
         |        c_contains:"4"
         |    }
         |    ]}
         |  }){
         |    childrenOpt {
         |      c
         |      test
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(2)

    server.query("query{parents{p,childrenOpt{c, test}}}", project).toString() should be(
      """{"data":{"parents":[{"p":"p1","childrenOpt":[{"c":"c1","test":null},{"c":"c2","test":null}]},{"p":"p2","childrenOpt":[{"c":"c3","test":null},{"c":"c4","test":null}]}]}}""")
  }

  private def setupData(project: Project) = {
    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: [{c: "c3"},{c: "c4"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }

}

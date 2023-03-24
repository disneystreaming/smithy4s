import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';
import CodeBlock from '@theme/CodeBlock';

const FeatureList = [
  {
    title: '1. Define API Contract',
    lang: "smithy",
    content: "service AdminService {\n  operations: [GetUser]\n}\n\noperation GetUser {\n  input := {\n    @required id: String\n  }\n  output := {\n    @required firstName: String\n    @required lastName: String\n  }\n}",
    description: (
      <>
        Start by defining your API in Smithy, a concise, readable, language-agnostic
        format.
      </>
    ),
  },
  {
    title: '2. Implement Generated Interface',
    lang: "scala",
    content: "object AdminServiceImpl extends AdminService[IO] {\n  def getUser(id: String): IO[GetUserOutput] = ...\n}",
    description: (
      <>
        Smithy4s will use the Smithy model you define to generate Scala code including an interface that represents the service. This interface will contain one function per operation in the service.
      </>
    ),
  },
  {
    title: '3. Create HttpRoutes',
    lang: "scala",
    content: "val routes: Resource[IO, HttpRoutes[IO]] =\n  SimpleRestJsonBuilder.routes(AdminServiceImpl).resource",
    description: (
      <>
        Passing your service implementation to the SimpleRestJsonBuilder will create an HttpRoutes instance that handles routing and JSON serialization/deserialization.
      </>
    ),
  },
];

function Feature({ lang, content, title, description }) {
  return (
    <div className={clsx(styles.doubleGrid)}>
      <div className="">
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
      <CodeBlock
        className={clsx(styles.codeExample)}
        language={lang}>
        {content}
      </CodeBlock>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container" style={{marginTop: "28px"}}>
        {FeatureList.map((props, idx) => (
          <Feature key={idx} {...props} />
        ))}
      </div>
    </section>
  );
}

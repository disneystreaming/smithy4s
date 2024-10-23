import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';
import CodeBlock from '@theme/CodeBlock';
import HelloWorldSmithy from 'raw-loader!./sample_data/HelloWorld.smithy';
import HelloWorldRoutes from 'raw-loader!./sample_data/HelloWorldRoutes.scala';
import HelloWorldImpl from 'raw-loader!./sample_data/HelloWorldImpl.scala';

const FeatureList = [
  {
    title: '1. Define API Contract',
    lang: "smithy",
    content: HelloWorldSmithy,
    description: (
      <>
        Start by defining your API in <b>Smithy</b>, a concise, readable, language-agnostic
        format.

        <br />
        <br />
          Smithy is <b>protocol-agnostic</b>, which means that you can use it to describe operations
          regardless of serialisation/transport.

        <br />
        <br />
          Smithy provides core semantics to define data types, operations and services. It also provides
          a powerful and extensible annotation mechanism, called <b>traits </b> to tie those definitions
          to <b>protocols</b> or validation rules.

        <br />
        <br />
          Smithy comes with a standard library of protocol-related traits (http/json/xml).
      </>
    ),
  },
  {
    title: '2. Implement Generated Interface',
    lang: "scala",
    content: HelloWorldImpl,
    description: (
      <>
        <b>Smithy4s</b> uses the Smithy model you define to generate Scala code, including an interface that represents the service. This interface contains one method per service operation.
      </>
    ),
  },
  {
    title: '3. Transform Into HttpRoutes',
    lang: "scala",
    content: HelloWorldRoutes,
    description: (
      <>
        Passing your service implementation to the SimpleRestJsonBuilder gives you an HttpRoutes instance that handles HTTP routing and JSON serialization/deserialization.
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
      <div className="container" style={{ marginTop: "28px" }}>
        {FeatureList.map((props, idx) => (
          <Feature key={idx} {...props} />
        ))}
      </div>
    </section>
  );
}

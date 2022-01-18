import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';

const FeatureList = [
  {
    title: 'Contract First',
    Svg: require('../../static/img/undraw_sync_files.svg').default,
    description: (
      <>
        Smithy4s will generate Scala code from your Smithy files. This means your servers
        and clients will be generated from the same source of truth.
      </>
    ),
  },
  {
    title: 'Pure Functional Scala',
    Svg: require('../../static/img/undraw_programmer.svg').default,
    description: (
      <>
        All generated code is pure functional Scala. Smithy4s is designed to work
        seamlessly with all of your favorite frameworks.
      </>
    ),
  },
  {
    title: 'Documentation Generation',
    Svg: require('../../static/img/undraw_add_files.svg').default,
    description: (
      <>
        In addition to generating Scala code, Smithy4s can generate OpenAPI
        files. It allows you to easily serve these files through SwaggerUI.
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} alt={title} />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}

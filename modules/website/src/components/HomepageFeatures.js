import React from 'react';
import clsx from 'clsx';
import styles from './HomepageFeatures.module.css';

const FeatureList = [
  {
    title: 'Contract First',
    Svg: require('../../static/img/undraw_sync_files.svg').default,
    description: (
      <>
        Smithy4s generates protocol-agnostic Scala code from Smithy, a concise, readable, language-agnostic
        format.
      </>
    ),
  },
  {
    title: 'Pure Functional Scala',
    Svg: require('../../static/img/undraw_programmer.svg').default,
    description: (
      <>
        Smithy4s allows for idiomatic integration with your favourite
        libraries/frameworks.
      </>
    ),
  },
  {
    title: 'Seamless API Dev',
    Svg: require('../../static/img/undraw_add_files.svg').default,
    description: (
      <>
        Smithy4s can translate your specs to OpenAPI files, and makes it trivial to serve them.
      </>
    ),
  },
];

function Feature({ Svg, title, description }) {
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

import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';
import HomepageFeatures from '../components/HomepageFeatures';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <div style={{display: 'flex', justifyContent: 'center', paddingTop: 20, paddingBottom: 10}}>
          <img className={styles.logoBackground} src="img/logo.svg" />
          <h1 className={styles.headerTitle} style={{textAlign: "start", paddingBottom: 0, marginBottom: 0, paddingLeft: 20}}>{siteConfig.title}</h1>
        </div>
        <p className={styles.headerSubtitle} style={{textAlign: "center", paddingTop: 0, marginTop: 0}}>{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/overview/intro">
            Get Started
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title}`}
      description="smithy4s is a framework for generating Scala code from Smithy files">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}

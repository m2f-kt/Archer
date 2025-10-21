import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';

type FeatureItem = {
  title: string;
  Svg: React.ComponentType<React.ComponentProps<'svg'>>;
  description: JSX.Element;
};

const FeatureList: FeatureItem[] = [
  {
    title: 'Less Boilerplate',
    Svg: require('@site/static/img/undraw_docusaurus_mountain.svg').default,
    description: (
      <>
        Eliminate the need for countless interfaces and implementations.
        Archer provides contractual DataSources and Repositories that reduce
        boilerplate while maintaining clean architecture principles.
      </>
    ),
  },
  {
    title: 'Functional & Type-Safe',
    Svg: require('@site/static/img/undraw_docusaurus_tree.svg').default,
    description: (
      <>
        Built on Arrow, Archer brings functional programming patterns to
        Clean Architecture. Use Either, Ice, or Nullable for type-safe
        error handling with <code>raise</code> and typed errors.
      </>
    ),
  },
  {
    title: 'Flexible Strategies',
    Svg: require('@site/static/img/undraw_docusaurus_react.svg').default,
    description: (
      <>
        Compose data sources with simple DSL. Add caching, expiration,
        validation, and more. Switch between NetworkFirst, StoreFirst,
        or custom strategies with minimal code changes.
      </>
    ),
  },
];

function Feature({title, Svg, description}: FeatureItem) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <Heading as="h3">{title}</Heading>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures(): JSX.Element {
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

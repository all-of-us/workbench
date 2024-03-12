import * as React from 'react';

import { styles } from './styles';

interface ArticleLink {
  text: string;
  link: string;
}

export const SupportNoteSection = (
  appName: string,
  supportArticles: ArticleLink[]
) => {
  return (
    <div style={{ ...styles.controlSection }}>
      <div style={{ fontWeight: 'bold' }}>{appName} support articles</div>
      {supportArticles.map((article, index) => (
        <div key={index} style={{ display: 'block' }}>
          <a href={article.link} target='_blank'>
            {index + 1}. {article.text}
          </a>
        </div>
      ))}
    </div>
  );
};

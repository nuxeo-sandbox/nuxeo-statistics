/**
(C) Copyright Nuxeo Corp. (http://nuxeo.com/)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import { PolymerElement } from '@polymer/polymer/polymer-element.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { mixinBehaviors } from '@polymer/polymer/lib/legacy/class.js';
import { I18nBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-i18n-behavior.js';
import '@nuxeo/chart-elements/chart-line.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-card.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-date-picker.js';
import '@nuxeo/nuxeo-elements/nuxeo-operation.js';
import './nuxeo-statistics-data.js';

const _buildTS = (data) => {
  const ts = [];
  const datasets = {};
  data.forEach((e) => {
    Object.entries(e).forEach(([k, v]) => {
      if (k === 'ts') {
        ts.push(v);
      } else {
        if (!datasets[k]) {
          datasets[k] = [];
        }
        datasets[k].push(v);
      }
    });
  });
  return {
    labels: ts,
    values: Object.values(datasets),
    series: Object.keys(datasets),
  };
};

class StatisticsPage extends mixinBehaviors([I18nBehavior], PolymerElement) {
  static get template() {
    return html`
      <style include="iron-flex iron-flex-alignment">
        :host {
          display: flex;
          flex-wrap: wrap;
          padding: 0 8px;
        }

        nuxeo-card {
          flex: 1 0 calc(33% - 2em);
          margin: 0 8px 16px;
          text-align: center;
        }

        chart-line,
        chart-pie {
          margin: 25px auto 0 auto;
          width: 100% !important;
          min-width: 30em;
          display: block;
          font-size: 0.8rem;
        }
      </style>

      <nuxeo-statistics-data
        metric="repository.documents.(.+).default"
        data="{{documentsStats}}"
      ></nuxeo-statistics-data>

      <!-- Document count per type -->
      <nuxeo-card heading="[[i18n('statistics.documentTypes.heading')]]">
        <chart-pie
          values="[[_values(documents)]]"
          labels="[[_labels(documents)]]"
          options='{ "legend": { "display": true, "position": "bottom", "labels": { "boxWidth": 12 } }, "animation": false }'
        >
        </chart-pie>
      </nuxeo-card>

      <!-- Document count per type over time -->
      <nuxeo-card heading="[[i18n('statistics.documentTypes.heading')]]">
        <chart-line
          values="[[documentsTS.values]]"
          labels="[[documentsTS.labels]]"
          series="[[documentsTS.series]]"
        ></chart-line>
      </nuxeo-card>
    `;
  }

  static get is() {
    return 'nuxeo-statistics-page';
  }

  static get properties() {
    return {
      documentsStats: {
        type: Object,
        observer: '_documentsStatsChanged',
      },
      documentsTS: Object,
      documents: Object,
    };
  }

  _labels(obj) {
    return Object.keys(obj);
  }

  _values(obj) {
    return Object.values(obj);
  }

  _documentsStatsChanged(data) {
    this.documentsTS = _buildTS(data);
    const documents = { ...data[data.length - 1] };
    delete documents.ts;
    this.documents = documents;
  }
}

customElements.define(StatisticsPage.is, StatisticsPage);

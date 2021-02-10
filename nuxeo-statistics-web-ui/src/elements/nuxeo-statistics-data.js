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
import '@nuxeo/nuxeo-elements/nuxeo-element.js';
import '@nuxeo/nuxeo-elements/nuxeo-operation.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import moment from '@nuxeo/moment';

/**
 * An element providing statistics data.
 * @memberof Nuxeo
 */
class StatisticsData extends Nuxeo.Element {
  static get is() {
    return 'nuxeo-statistics-data';
  }

  static get properties() {
    return {
      filter: {
        type: String,
        value: '*',
      },
      metric: {
        type: String,
      },
      dateFormat: {
        type: String,
        value: 'Y-MM-DD H:m',
      },
      data: {
        type: Object,
        notify: true,
      },
    };
  }

  static get observers() {
    return ['_doFetch(filter, groupedBy)'];
  }

  static get template() {
    return html` <nuxeo-operation id="op" op="Statistics.Fetch"></nuxeo-operation> `;
  }

  ready() {
    super.ready();
    this.fetch();
  }

  fetch() {
    this._op = this.$.op;
    const params = {};
    let { filter } = this;
    if (this.metric) {
      filter = `nuxeo.statistics.${this.metric}`;
    }
    if (filter !== '*') {
      params.filter = filter;
    }
    this._op.params = params;
    this._op.execute().then((response) => {
      this.data = JSON.parse(response.value).map((e) =>
        Object.fromEntries(
          Object.entries(e).map(([k, v]) => {
            if (k === 'ts') {
              return ['ts', moment(v * 1000).format(this.dateFormat)];
            }
            let key = k;
            if (this.metric) {
              // check is there's a capture group in the metric regex
              // if so, use it as key
              key = k.match(this.metric)[1] || k;
            }
            return [key.replace('nuxeo.statistics', ''), v];
          }),
        ),
      );
    });
  }

  _doFetch() {
    // template is not stamped & data system not initialized until the element has been connected :(
    if (this.isConnected) {
      this.fetch();
    }
  }
}

customElements.define(StatisticsData.is, StatisticsData);

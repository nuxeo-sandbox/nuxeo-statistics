/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *      Nelson Silva
 */
package org.nuxeo.statistics.repository;

import java.util.Map;
import java.util.function.Supplier;

public abstract class RepositoryStatisticsComputer implements Supplier<Map<String, Long>> {
    public abstract long getTotalDocuments();

    public abstract long getDeletedDocuments();

    @Override
    public Map<String, Long> get() {
        return Map.of(
                "documents.total", getTotalDocuments(),
                "documents.deleted", getDeletedDocuments()
        );
    }
}

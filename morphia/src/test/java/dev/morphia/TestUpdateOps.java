/*
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package dev.morphia;

import com.mongodb.client.result.UpdateResult;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PreLoad;
import dev.morphia.annotations.Reference;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.TestQuery.ContainsPic;
import dev.morphia.query.TestQuery.Pic;
import dev.morphia.query.Update;
import dev.morphia.query.ValidationException;
import dev.morphia.query.internal.MorphiaCursor;
import dev.morphia.testmodel.Article;
import dev.morphia.testmodel.Circle;
import dev.morphia.testmodel.Rectangle;
import dev.morphia.testmodel.Translation;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.morphia.query.PushOptions.options;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestUpdateOps extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TestUpdateOps.class);

    @Test
    public void shouldUpdateAnArrayElement() {
        // given
        ObjectId parentId = new ObjectId();
        String childName = "Bob";
        String updatedLastName = "updatedLastName";

        Parent parent = new Parent();
        parent.id = parentId;
        parent.children.add(new Child("Anthony", "Child"));
        parent.children.add(new Child(childName, "originalLastName"));
        getDs().save(parent);

        // when
        Query<Parent> query = getDs().find(Parent.class)
                                     .field("_id").equal(parentId)
                                     .field("children.first")
                                     .equal(childName);
        UpdateResult updateResult = query.update()
                                         .set("children.$.last", updatedLastName)
                                         .execute();

        // then
        assertThat(updateResult.getModifiedCount(), is(1L));
        assertThat(getDs().find(Parent.class).filter("id", parentId)
                          .execute(new FindOptions().limit(1))
                          .next()
                       .children, hasItem(new Child(childName, updatedLastName)));
    }

    @Test
    public void testAdd() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);

        assertThat(get(cIntArray), is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().createQuery(ContainsIntArray.class);
        //add 4 to array
        assertUpdated(query.update()
                           .addToSet("values", 4)
                           .execute(),
            1);

        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4}));

        //add unique (4) -- noop
        assertEquals(1, query.update().addToSet("values", 4).execute().getMatchedCount());
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4}));

        //add dup 4
        assertUpdated(query.update().push("values", 4).execute(), 1);
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 4}));

        //cleanup for next tests
        getDs().find(ContainsIntArray.class).delete();
        cIntArray = getDs().find(ContainsIntArray.class)
                           .filter("_id", getDs().save(new ContainsIntArray()).id)
                           .first();

        //add [4,5]
        final List<Integer> newValues = new ArrayList<>();
        newValues.add(4);
        newValues.add(5);

        assertUpdated(query.update().addToSet("values", newValues).execute(), 1);
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 5}));

        //add them again... noop
        assertEquals(1, query.update().addToSet("values", newValues).execute().getMatchedCount());
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 5}));

        //add dups [4,5]
        assertUpdated(query.update().push("values", newValues).execute(), 1);
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 5, 4, 5}));
    }

    private Integer[] get(final ContainsIntArray array) {
        return getDs().find(ContainsIntArray.class)
                      .filter("_id", array.id)
                      .first()
            .values;
    }

    @Test
    public void testAddAll() {
        getMapper().map(LogHolder.class, Log.class);
        String uuid = "4ec6ada9-081a-424f-bee0-934c0bc4fab7";

        LogHolder logs = new LogHolder();
        logs.uuid = uuid;
        getDs().save(logs);

        Query<LogHolder> finder = getDs().find(LogHolder.class).field("uuid").equal(uuid);

        // both of these entries will have a className attribute
        List<Log> latestLogs = asList(new Log(1), new Log(2));

        finder.update()
              .addToSet("logs", latestLogs)
              .execute(new UpdateOptions()
                           .upsert(true));
        LogHolder first = finder.first();
        validateClassName(first);

        // this entry will NOT have a className attribute
        Log log = new Log(3);
        finder
            .update()
            .addToSet("logs", log)
            .execute(new UpdateOptions().upsert(true));
        validateClassName(finder.first());

        // this entry will NOT have a className attribute
        finder.update()
              .addToSet("logs", new Log(4))
              .execute(new UpdateOptions().upsert(true));
        validateClassName(finder.first());
    }

    @Test
    public void testMultiUpdates() {
        getMapper().map(ContainsPic.class);
        Query<ContainsPic> finder = getDs().find(ContainsPic.class);

        createContainsPic(0);
        createContainsPic(1);
        createContainsPic(2);

        finder.update()
              .inc("size")
              .execute( new UpdateOptions().multi(true));

        final MorphiaCursor<ContainsPic> iterator = finder.execute(new FindOptions().sort(Sort.ascending("size")));
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, iterator.next().getSize());
        }
    }

    public void createContainsPic(final int size) {
        final ContainsPic containsPic = new ContainsPic();
        containsPic.setSize(size);
        getDs().save(containsPic);
    }


    @Test
    public void testAddToSet() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class)
                                               .filter("_id", cIntArray.id);
        
        assertThat(query.first().values, is((new ContainsIntArray()).values));

        assertUpdated(query.update().addToSet("values", 5).execute(), 1);
        
        assertThat(query.first().values, is(new Integer[]{1, 2, 3, 5}));

        assertUpdated(query.update().addToSet("values", 4).execute(), 1);
        assertThat(query.first().values, is(new Integer[]{1, 2, 3, 5, 4}));

        assertUpdated(query.update().addToSet("values", asList(8, 9)).execute(), 1);
        assertThat(query.first().values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9}));

        assertEquals(1, query.update().addToSet("values", asList(4, 5)).execute().getMatchedCount());
        assertThat(query.first().values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9}));

        assertUpdated(query.update().addToSet("values", new HashSet<>(asList(10, 11))).execute(), 1);
        assertThat(query.first().values, is(new Integer[]{1, 2, 3, 5, 4, 8, 9, 10, 11}));
    }

    @Test
    public void testUpsert() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        ContainsIntArray control = new ContainsIntArray();
        getDs().save(asList(cIntArray, control));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);

        doUpdates(cIntArray, control, query.update().addToSet("values", 4),
                  new Integer[]{1, 2, 3, 4});


        doUpdates(cIntArray, control, query.update().addToSet("values", asList(4, 5)),
                  new Integer[]{1, 2, 3, 4, 5});


        assertInserted(getDs().find(ContainsIntArray.class)
                              .filter("values", new Integer[]{4, 5, 7})
                              .update()
                              .addToSet("values", 6)
                              .execute(new UpdateOptions().upsert(true)));

        query = getDs().find(ContainsIntArray.class)
                       .filter("values", new Integer[]{4, 5, 7, 6});
        FindOptions options = new FindOptions()
                                  .logQuery();
        ContainsIntArray values = query.first(options);
        assertNotNull(getDs().getLoggedQuery(options), values);
    }

    private void doUpdates(final ContainsIntArray updated, final ContainsIntArray control, final Update update, final Integer[] target) {
        assertUpdated(update.execute(new UpdateOptions()), 1);
        assertThat((getDs().find(ContainsIntArray.class)
                           .filter("_id", updated.id)
                           .first()).values,
            is(target));
        assertThat((getDs().find(ContainsIntArray.class)
                           .filter("_id", control.id)
                           .first()).values,
            is(new Integer[]{1, 2, 3}));

        assertEquals(1, update.execute(new UpdateOptions()).getMatchedCount());
        assertThat((getDs().find(ContainsIntArray.class)
                           .filter("_id", updated.id)
                           .first()).values,
            is(target));
        assertThat((getDs().find(ContainsIntArray.class)
                           .filter("_id", control.id)
                           .first()).values,
            is(new Integer[]{1, 2, 3}));
    }

    @Test
    public void testExistingUpdates() {
        getDs().save(new Circle(100D));
        getDs().save(new Circle(12D));
        Query<Circle> circle = getDs().find(Circle.class);
        assertUpdated(circle.update().inc("radius", 1D).execute(), 1);

        assertUpdated(circle.update().inc("radius").execute(new UpdateOptions().multi(true)), 2);

        //test possible data type change.
        final Circle updatedCircle = circle.filter("radius", 13)
                                            .execute(new FindOptions().limit(1))
                                            .next();
        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(13D));
    }

    @Test
    public void testIncDec() {
        final Rectangle[] array = {
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(1, 10),
            new Rectangle(10, 10),
            new Rectangle(10, 10)};

        for (final Rectangle rect : array) {
            getDs().save(rect);
        }

        final Query<Rectangle> heightOf1 = getDs().find(Rectangle.class).filter("height", 1D);
        final Query<Rectangle> heightOf2 = getDs().find(Rectangle.class).filter("height", 2D);
        final Query<Rectangle> heightOf35 = getDs().find(Rectangle.class).filter("height", 3.5D);

        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        UpdateResult results = heightOf1
                                   .update()
                                   .inc("height")
                                   .execute(new UpdateOptions().multi(true));
        assertUpdated(results, 3);

        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf2.count(), is(3L));

        heightOf2.update().dec("height").execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf2.count(), is(0L));

        heightOf1.update().inc("height", 2.5D).execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(0L));
        assertThat(heightOf35.count(), is(3L));

        heightOf35.update().dec("height", 2.5D).execute(new UpdateOptions().multi(true));
        assertThat(heightOf1.count(), is(3L));
        assertThat(heightOf35.count(), is(0L));

        getDs().find(Rectangle.class).filter("height", 1D)
               .update()
               .set("height", 1D)
               .inc("width", 20D)
               .execute();

        assertThat(getDs().find(Rectangle.class).count(), is(5L));
        assertThat(getDs().find(Rectangle.class).filter("height", 1D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));
        assertThat(getDs().find(Rectangle.class).filter("width", 30D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));

        getDs().find(Rectangle.class).filter("width", 30D)
               .update()
               .set("height", 2D).set("width", 2D)
               .execute();
        assertThat(getDs().find(Rectangle.class).filter("width", 1D)
                          .execute(new FindOptions().limit(1))
                          .tryNext(), is(nullValue()));
        assertThat(getDs().find(Rectangle.class).filter("width", 2D)
                          .execute(new FindOptions().limit(1))
                          .next(), is(notNullValue()));

        heightOf35.update().dec("height", 1).execute();
        heightOf35.update().dec("height", Long.MAX_VALUE).execute();
        heightOf35.update().dec("height", 1.5f).execute();
        heightOf35.update().dec("height", Double.MAX_VALUE).execute();
        try {
            heightOf35.update()
                      .dec("height", new AtomicInteger(1));
            fail("Wrong data type not recognized.");
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Test
    public void testInsertUpdate() {
        assertInserted(getDs().find(Circle.class).field("radius").equal(0)
                              .update()
                              .inc("radius", 1D)
                              .execute(new UpdateOptions().upsert(true)));
    }

    @Test
    @Category(Reference.class)
    public void testInsertWithRef() {
        final Pic pic = new Pic();
        pic.setName("fist");
        final ObjectId picKey = getDs().save(pic).getId();

        Query<ContainsPic> query = getDs().find(ContainsPic.class).filter("name", "first").filter("pic", picKey);
        assertInserted(query.update()
                            .set("name", "A")
                            .execute(new UpdateOptions().upsert(true)));
        assertThat(getDs().find(ContainsPic.class).count(), is(1L));
        getDs().delete(getDs().find(ContainsPic.class));

        query = getDs().find(ContainsPic.class).filter("name", "first").filter("pic", pic);
        assertInserted(query.update()
                            .set("name", "second")
                            .execute(new UpdateOptions().upsert(true)));
        assertThat(getDs().find(ContainsPic.class).count(), is(1L));

        //test reading the object.
        final ContainsPic cp = getDs().find(ContainsPic.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cp, is(notNullValue()));
        assertThat(cp.getName(), is("second"));
        assertThat(cp.getPic(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is(notNullValue()));
        assertThat(cp.getPic().getName(), is("fist"));
    }

    @Test
    public void testMaxKeepsCurrentDocumentValueWhenThisIsLargerThanSuppliedValue() {
        final ObjectId id = new ObjectId();
        final double originalValue = 2D;

        Datastore ds = getDs();
        Query<Circle> query = ds.find(Circle.class)
                              .field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", originalValue)
                            .execute(new UpdateOptions().upsert(true)));

        assertEquals(1, query.update()
                             .max("radius", 1D)
                             .execute(new UpdateOptions().upsert(true)).getMatchedCount());

        assertThat(ds.find(Circle.class).filter("_id", id).first().getRadius(), is(originalValue));
    }

    @Test
    public void testPush() {
        ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        assertThat(get(cIntArray), is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        query.update()
             .push("values", 4)
             .execute();

        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4}));

        query.update()
             .push("values", 4)
             .execute();
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 4}));

        query.update()
             .push("values", asList(5, 6))
             .execute();
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 3, 4, 4, 5, 6}));

        query.update()
             .push("values", 12, options().position(2))
             .execute();

        assertThat(get(cIntArray), is(new Integer[]{1, 2, 12, 3, 4, 4, 5, 6}));


        query.update()
             .push("values", asList(99, 98, 97), options().position(4))
             .execute();
        assertThat(get(cIntArray), is(new Integer[]{1, 2, 12, 3, 99, 98, 97, 4, 4, 5, 6}));
    }

    @Test
    public void testRemoveAllSingleValue() {
        LogHolder logs = new LogHolder();
        Date date = new Date();
        logs.logs.addAll(asList(
            new Log(1),
            new Log(2),
            new Log(3),
            new Log(1),
            new Log(2),
            new Log(3)));

        Datastore ds = getDs();
        ds.save(logs);

        UpdateResult results = ds.find(LogHolder.class).update()
                                 .removeAll("logs", new Log(3))
                                 .execute();

        assertEquals(1, results.getModifiedCount());
        LogHolder updated = ds.find(LogHolder.class)
                              .execute(new FindOptions().limit(1))
                              .next();
        assertEquals(4, updated.logs.size());
        assertTrue(updated.logs.stream()
                               .allMatch(log ->
                                             log.equals(new Log(1))
                                             || log.equals(new Log(2))));
    }

    @Test
    public void testRemoveAllList() {
        LogHolder logs = new LogHolder();
        Date date = new Date();
        logs.logs.addAll(asList(
            new Log(1),
            new Log(2),
            new Log(3),
            new Log(1),
            new Log(2),
            new Log(3)));

        Datastore ds = getDs();
        ds.save(logs);

        UpdateResult results = ds.find(LogHolder.class).update()
                                 .removeAll("logs", singletonList(new Log(3)))
                                 .execute();

        assertEquals(1, results.getModifiedCount());
        LogHolder updated = ds.find(LogHolder.class)
                              .execute(new FindOptions().limit(1))
                              .next();
        assertEquals(4, updated.logs.size());
        assertTrue(updated.logs.stream()
                               .allMatch(log ->
                                             log.equals(new Log(1))
                                             || log.equals(new Log(2))));
    }

    @Test
    public void testRemoveWithNoData() {
        DumbColl dumbColl = new DumbColl("ID");
        dumbColl.fromArray = singletonList(new DumbArrayElement("something"));
        DumbColl dumbColl2 = new DumbColl("ID2");
        dumbColl2.fromArray = singletonList(new DumbArrayElement("something"));
        getDs().save(asList(dumbColl, dumbColl2));

        UpdateResult deleteResults = getDs().find(DumbColl.class)
                                            .field("opaqueId").equalIgnoreCase("ID")
                                            .update()
                                            .pull("fromArray", new Document("whereId", "not there"))
                                            .execute();

        final UpdateResult execute = getDs().find(DumbColl.class).field("opaqueId").equalIgnoreCase("ID")
                                             .update()
                                             .removeAll("fromArray", new DumbArrayElement("something"))
                                             .execute();
    }

    @Test
    public void testElemMatchUpdate() {
        // setUp
        Object id = getDs().save(new ContainsIntArray()).id;
        assertThat(getDs().find(ContainsIntArray.class).filter("_id", id).first().values, arrayContaining(1, 2, 3));

        // do patch

        getDs().find(ContainsIntArray.class)
               .filter("id", id)
               .filter("values", 2)
               .update()
               .set("values.$", 5)
               .execute();

        // expected
        assertThat(getDs().find(ContainsIntArray.class).filter("_id", id).first().values, arrayContaining(1, 5, 3));
    }

    @Test
    public void testRemoveFirst() {
        final ContainsIntArray cIntArray = new ContainsIntArray();
        getDs().save(cIntArray);
        Integer[] values = get(cIntArray);
        assertThat(values.length, is(3));
        assertThat(values, is((new ContainsIntArray()).values));

        Query<ContainsIntArray> query = getDs().find(ContainsIntArray.class);
        assertUpdated(query.update().removeFirst("values").execute(), 1);
        assertThat(get(cIntArray), is(new Integer[]{2, 3}));

        assertUpdated(query.update().removeLast("values").execute(), 1);
        assertThat(get(cIntArray), is(new Integer[]{2}));
    }

    @Test
    public void testSetOnInsertWhenInserting() {
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs().find(Circle.class).field("id").equal(id);
        assertInserted(query.update()
                            .setOnInsert("radius", 2D)
                            .execute(new UpdateOptions().upsert(true)));

        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();

        assertThat(updatedCircle, is(notNullValue()));
        assertThat(updatedCircle.getRadius(), is(2D));
    }

    @Test
    public void testSetOnInsertWhenUpdating() {
        ObjectId id = new ObjectId();

        Query<Circle> query = getDs()
                                  .find(Circle.class)
                                  .field("id")
                                  .equal(id);

        assertInserted(query.update()
                            .setOnInsert("radius", 1D)
                            .execute(new UpdateOptions()
                                         .upsert(true)));

        assertEquals(1, query.update()
                             .setOnInsert("radius", 2D)
                             .execute(new UpdateOptions()
                                          .upsert(true)).getMatchedCount());

        final Circle updatedCircle = getDs().find(Circle.class).filter("_id", id).first();

        assertNotNull(updatedCircle);
        assertEquals(1D, updatedCircle.getRadius(), 0.1);
    }

    @Test
    @Category(Reference.class)
    public void testSetUnset() {
        Datastore ds = getDs();
        final ObjectId key = ds.save(new Circle(1)).getId();

        Query<Circle> circle = ds.find(Circle.class).filter("radius", 1D);
        assertUpdated(circle.update().set("radius", 2D).execute(), 1);

        Query<Circle> idQuery = ds.find(Circle.class)
                             .filter("_id", key);
        assertThat(idQuery.first().getRadius(), is(2D));

        circle = ds.find(Circle.class).filter("radius", 2D);
        assertUpdated(circle.update().unset("radius")
                            .execute(new UpdateOptions().multi(false)), 1);

        assertThat(idQuery.first().getRadius(), is(0D));

        Article article = new Article();

        ds.save(article);

        Query<Article> query = ds.find(Article.class);
        query.update()
             .set("translations", new HashMap<String, Translation>())
             .execute();

        query.update()
             .unset("translations")
             .execute();
    }

    @Test
    public void testUpdateFirstNoCreate() {
        getDs().delete(getDs().find(LogHolder.class));
        List<LogHolder> logs = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            logs.add(createEntryLogs("logs" + i));
        }
        LogHolder logs1 = logs.get(0);
        Query<LogHolder> query = getDs().find(LogHolder.class);
        Document object = new Document("new", "value");
        query.update()
             .set("raw", object)
             .execute();

        List<LogHolder> list = getDs().find(LogHolder.class).execute().toList();
        for (int i = 0; i < list.size(); i++) {
            final LogHolder logHolder = list.get(i);
            assertEquals(logHolder.id.equals(logs1.id) ? object : logs.get(i).raw, logHolder.raw);
        }
    }

    @Test
    @Category(Reference.class)
    public void testUpdateKeyRef() {
        final ContainsPicKey cpk = new ContainsPicKey();
        cpk.name = "cpk one";

        Datastore ds = getDs();
        ds.save(cpk);

        final Pic pic = new Pic();
        pic.setName("fist again");
        ds.save(pic);

        Query<ContainsPicKey> query = ds.find(ContainsPicKey.class).filter("name", cpk.name);
        assertThat(query.update()
                        .set("pic", pic)
                        .execute().getModifiedCount(), is(1L));

        //test reading the object.
        final ContainsPicKey cpk2 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk2, is(notNullValue()));
        assertThat(cpk.name, is(cpk2.name));
        assertThat(cpk2.pic, is(notNullValue()));
        assertThat(pic, is(cpk2.pic.get()));

        query.update().set("pic", pic).execute();

        //test reading the object.
        final ContainsPicKey cpk3 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk3, is(notNullValue()));
        assertThat(cpk.name, is(cpk3.name));
        assertThat(cpk3.pic, is(notNullValue()));
        assertThat(pic, is(cpk3.pic.get()));
    }

    @Test
    public void testUpdateKeyList() {
        final ContainsPicKey cpk = new ContainsPicKey();
        cpk.name = "cpk one";

        Datastore ds = getDs();
        ds.save(cpk);

        final Pic pic = new Pic();
        pic.setName("fist again");
        ds.save(pic);

        cpk.keys = MorphiaReference.wrap(List.of(pic));

        //test with Key<Pic>
        Query<ContainsPicKey> query = ds.find(ContainsPicKey.class).filter("name", cpk.name);
        final UpdateResult res = query.update()
                                      .set("keys", cpk.keys).execute();

        assertThat(res.getModifiedCount(), is(1L));

        //test reading the object.
        final ContainsPicKey cpk2 = ds.find(ContainsPicKey.class)
                                      .execute(new FindOptions().limit(1))
                                      .next();
        assertThat(cpk2, is(notNullValue()));
        assertThat(cpk.name, is(cpk2.name));
        assertThat(cpk2.keys.get(), hasItem(pic));
    }

    @Test
    @Category(Reference.class)
    public void testUpdateRef() {
        final ContainsPic cp = new ContainsPic();
        cp.setName("cp one");

        getDs().save(cp);

        final Pic pic = new Pic();
        pic.setName("fist");
        getDs().save(pic);

        Query<ContainsPic> query = getDs().find(ContainsPic.class).filter("name", cp.getName());
        UpdateResult result = query.update()
                                   .set("pic", pic)
                                   .execute();
        assertEquals(result.getModifiedCount(), 1);

        //test reading the object.
        final ContainsPic cp2 = getDs().find(ContainsPic.class)
                                       .execute(new FindOptions().limit(1))
                                       .next();
        assertThat(cp2, is(notNullValue()));
        assertThat(cp.getName(), is(cp2.getName()));
        assertThat(cp2.getPic(), is(notNullValue()));
        assertThat(cp2.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp2.getPic().getName()));

        //test reading the object.
        final ContainsPic cp3 = getDs().find(ContainsPic.class)
                                       .execute(new FindOptions().limit(1))
                                       .next();
        assertThat(cp3, is(notNullValue()));
        assertThat(cp.getName(), is(cp3.getName()));
        assertThat(cp3.getPic(), is(notNullValue()));
        assertThat(cp3.getPic().getName(), is(notNullValue()));
        assertThat(pic.getName(), is(cp3.getPic().getName()));
    }

    @Test
    public void testUpdateWithDifferentType() {
        final ContainsInt cInt = new ContainsInt();
        cInt.val = 21;
        getDs().save(cInt);

        Query<ContainsInt> query = getDs().find(ContainsInt.class);

        final UpdateResult res = query.update().inc("val", 1.1D).execute();
        assertUpdated(res, 1);

        assertEquals(22, query.execute(new FindOptions()
                                           .limit(1))
                              .next().val);
    }

    @Test(expected = ValidationException.class)
    public void testValidationBadFieldName() {
        Query<Circle> query = getDs().find(Circle.class).field("radius").equal(0);
        query.update().inc("r", 1D).execute();
    }

    private void assertInserted(final UpdateResult res) {
        assertNotNull(res.getUpsertedId());
        assertEquals(0, res.getModifiedCount());
    }

    private void assertUpdated(final UpdateResult res, final long count) {
        assertEquals(count, res.getModifiedCount());
    }

    private LogHolder createEntryLogs(final String value) {
        LogHolder logs = new LogHolder();
        logs.raw = new Document("name", value);
        getDs().save(logs);

        return logs;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void validateClassName(final LogHolder loaded) {
        List<Document> logs = (List<Document>) loaded.raw.get("logs");
        for (Document o : logs) {
            Assert.assertNotNull(o.toString(), o.get(getMapper().getOptions().getDiscriminatorKey()));
        }
    }

    @Entity
    private static class ContainsIntArray {
        @Id
        private ObjectId id;
        private final Integer[] values = {1, 2, 3};
    }

    @Entity
    private static class ContainsInt {
        @Id
        private ObjectId id;
        private int val;
    }

    @Entity
    private static class ContainsPicKey {
        @Id
        private ObjectId id;
        private String name = "test";
        private MorphiaReference<Pic> pic;
        private MorphiaReference<List<Pic>> keys;
    }

    @Entity(useDiscriminator = false)
    public static class LogHolder {
        @Id
        private ObjectId id;
        @Indexed
        private String uuid;
        private Log log;
        private List<Log> logs = new ArrayList<>();
        private Document raw;

        @PreLoad
        public void preload(final Document raw) {
            this.raw = raw;
        }

        public List<Log> getLogs() {
            return logs;
        }

        public void setLogs(final List<Log> logs) {
            this.logs = logs;
        }

        public Log getLog() {
            return log;
        }

        public void setLog(final Log log) {
            this.log = log;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LogHolder)) {
                return false;
            }

            final LogHolder logHolder = (LogHolder) o;

            if (id != null ? !id.equals(logHolder.id) : logHolder.id != null) {
                return false;
            }
            if (uuid != null ? !uuid.equals(logHolder.uuid) : logHolder.uuid != null) {
                return false;
            }
            if (log != null ? !log.equals(logHolder.log) : logHolder.log != null) {
                return false;
            }
            if (logs != null ? !logs.equals(logHolder.logs) : logHolder.logs != null) {
                return false;
            }
            return raw != null ? raw.equals(logHolder.raw) : logHolder.raw == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
            result = 31 * result + (log != null ? log.hashCode() : 0);
            result = 31 * result + (logs != null ? logs.hashCode() : 0);
            result = 31 * result + (raw != null ? raw.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", LogHolder.class.getSimpleName() + "[", "]")
                       .add("id=" + id)
                       .add("uuid='" + uuid + "'")
                       .add("log=" + log)
                       .add("logs=" + logs)
                       .add("raw=" + raw)
                       .toString();
        }
    }

    @Embedded
    public static class Log {
        private long receivedTs;
        private String value;

        public Log() {
        }

        public Log(final long value) {
            this.value = "Log" + value;
            receivedTs = value;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Log)) {
                return false;
            }

            final Log log = (Log) o;

            if (receivedTs != log.receivedTs) {
                return false;
            }
            return value.equals(log.value);
        }

        @Override
        public int hashCode() {
            int result = (int) (receivedTs ^ (receivedTs >>> 32));
            result = 31 * result + value.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("EntityLog{receivedTs=%s, value='%s'}", receivedTs, value);
        }
    }

    @Entity
    private static final class Parent {
        @Id
        private ObjectId id;
        private final Set<Child> children = new HashSet<>();
    }

    @Embedded
    private static final class Child {
        private String first;
        private String last;

        private Child(final String first, final String last) {
            this.first = first;
            this.last = last;
        }

        private Child() {
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Child)) {
                return false;
            }

            final Child child = (Child) o;

            if (!Objects.equals(first, child.first)) {
                return false;
            }
            return Objects.equals(last, child.last);
        }

        @Override
        public int hashCode() {
            int result = first != null ? first.hashCode() : 0;
            result = 31 * result + (last != null ? last.hashCode() : 0);
            return result;
        }
    }

    @Entity
    private static final class DumbColl {
        @Id
        private ObjectId id;
        private String opaqueId;
        private List<DumbArrayElement> fromArray;

        private DumbColl() {
        }

        private DumbColl(final String opaqueId) {
            this.opaqueId = opaqueId;
        }
    }

    @Embedded
    private static final class DumbArrayElement {
        private String whereId;

        public DumbArrayElement() {
        }

        private DumbArrayElement(final String whereId) {
            this.whereId = whereId;
        }
    }

}

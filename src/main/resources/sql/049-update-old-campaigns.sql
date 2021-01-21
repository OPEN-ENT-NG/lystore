CREATE OR REPLACE FUNCTION lystore.update_old_end_date() RETURNS TRIGGER AS $$
BEGIN
         UPDATE lystore.campaign
         SET end_date = NULL
		 WHERE end_date <  NOW() AND automatic_close = true AND accessible = true;
		 RETURN NEW ;
END;
$$ LANGUAGE 'plpgsql';

UPDATE
  lystore.campaign
SET
  start_date = subCampaign.start_date,
  end_date = subCampaign.end_date
FROM
  (
    select
      Max(orders.creation_date) as end_date,
      MIN(orders.creation_date) as start_date,
      campaign.id
    from
      lystore.allOrders orders
      inner join lystore.campaign on orders.id_campaign = campaign.id
    group by
      campaign.id
  ) as subCampaign
where subCampaign.id = campaign.id and campaign.automatic_close = false and accessible = false;

UPDATE
  lystore.campaign
SET
  start_date = subCampaign.start_date
FROM
  (
    select
      MIN(orders.creation_date) as start_date,
      campaign.id
    from
      lystore.allOrders orders
      inner join lystore.campaign on orders.id_campaign = campaign.id
    group by
      campaign.id
  ) as subCampaign
where subCampaign.id = campaign.id and campaign.automatic_close = false and accessible = true
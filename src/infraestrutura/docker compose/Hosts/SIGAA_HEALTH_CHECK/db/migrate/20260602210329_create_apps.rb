class CreateApps < ActiveRecord::Migration[7.2]
  def change
    create_table :apps do |t|
      t.string :server
      t.string :db

      t.timestamps
    end
  end
end

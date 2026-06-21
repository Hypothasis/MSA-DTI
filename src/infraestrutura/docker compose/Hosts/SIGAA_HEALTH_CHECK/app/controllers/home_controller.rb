class HomeController < ApplicationController
  def index
    @AppStatus = App.find_by(id: 1)
  end

  def change
    app = App.first

    type = params[:type]
    status = params[:status]

    if type == "server"
      app.update(server: status)
    elsif type == "database"
      app.update(db: status)
    end

    head :ok
  end

  def health
      app = App.first
      if app.server == "UP" && app.db == "UP"
        render json: {status: "UP", deps: { db: "UP" }}
      elsif app.server == "UP" && app.db == "DOWN"
        render json: {status: "UP", deps: { db: "DOWN" }}
      else
        render json: {status: "Service Unavailable"}
      end
  end
end
